package co.casterlabs.kaimen.app;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import co.casterlabs.kaimen.app.IpcPacket.IpcPacketType;
import co.casterlabs.kaimen.app.IpcPacketInvocationResult.ResultData;
import co.casterlabs.kaimen.app.IpcPacketPrint.PrintChannel;
import co.casterlabs.kaimen.util.threading.AsyncTask;
import co.casterlabs.kaimen.util.threading.Promise;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class IpcHostHandler {
    private static final FastLogger LOGGER = new FastLogger();

    static {
        LOGGER.setCurrentLevel(LogLevel.NONE); // Stop it.
    }

    @SuppressWarnings({
            "unchecked",
            "deprecation"
    })
    public static <T> T startInstance(@NonNull Class<? extends IpcObject> implementingClazz) throws InterruptedException, Throwable {
        final Class<?> interfaceClazz = implementingClazz.newInstance().getInterfaceClass();

        LOGGER.debug("Starting IPC for %s with %s", interfaceClazz, implementingClazz);

        Process process = new ProcessBuilder()
            .command(getExec("co.casterlabs.kaimen.app.IpcClientHandler"))
            .redirectOutput(Redirect.PIPE)
            .redirectError(Redirect.DISCARD) // Unused.
            .redirectInput(Redirect.PIPE)
            .start();

        Promise<Void> startPromise = new Promise<>();

        ProxyHandler handler = new ProxyHandler();
        Host host = new Host() {
            @Override
            public void sendPacket(IpcPacket packet) {
                try {
                    String packetString = Rson.DEFAULT
                        .toJson(packet)
                        .toString();

                    LOGGER.debug("Sending packet:\n%s", packetString);

                    process.getOutputStream().write(packetString.getBytes());
                    process.getOutputStream().write('\n');
                    process.getOutputStream().flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    process.destroy();
                }
            }
        };

        handler.host = host;

        host.sendPacket(
            new IpcPacketInit()
                .setTargetClass(implementingClazz.getCanonicalName())
        );

        new AsyncTask(() -> {
            try {
                process.waitFor();
            } catch (InterruptedException e) {}

            LOGGER.debug("Ipc closed.");

            host.isAlive = false;
        });

        new AsyncTask(() -> {
            try (Scanner in = new Scanner(process.getInputStream())) {
                while (host.isAlive) {
                    String line = in.nextLine();

                    new AsyncTask(() -> {
                        try {
                            IpcPacket packet = IpcPacketType.parsePacket(
                                Rson.DEFAULT.fromJson(line, JsonObject.class)
                            );

                            LOGGER.debug("Received packet:\n%s", line);

                            switch (packet.getType()) {
                                case CLIENT_READY: {
                                    IpcPacketClientReady clientReadyPacket = (IpcPacketClientReady) packet;

                                    handler.objectId = clientReadyPacket.getObjectId();
                                    startPromise.fulfill(null);
                                    break;
                                }

                                case INVOCATION_RESULT: {
                                    IpcPacketInvocationResult resultPacket = (IpcPacketInvocationResult) packet;
                                    Promise<ResultData> promise = handler.waiting.get(resultPacket.getInvocationId());

                                    if (resultPacket.isError()) {
                                        promise.error(new Exception("\n" + resultPacket.getError()));
                                    } else {
                                        promise.fulfill(resultPacket.getResult());
                                    }
                                    break;
                                }

                                case PRINT: {
                                    IpcPacketPrint printPacket = (IpcPacketPrint) packet;

                                    if (printPacket.getChannel() == PrintChannel.STDOUT) {
                                        System.out.write(printPacket.getBytes());
                                        System.out.write('\n');
                                    } else {
                                        System.err.write(printPacket.getBytes());
                                        System.err.write('\n');
                                    }
                                    break;
                                }

                                default:
                                    break;
                            }
                        } catch (Exception e) {
                            FastLogger.logStatic(LogLevel.SEVERE, "An error occured whilst processing packet: %s\n%s", line, e);
                        }
                    });
                }
            }
        });

        startPromise.await();

        return (T) Proxy.newProxyInstance(
            interfaceClazz.getClassLoader(),
            arr(interfaceClazz),
            handler
        );
    }

    private static abstract class Host {
        private boolean isAlive = true;

        public abstract void sendPacket(IpcPacket packet);

    }

    private static class ProxyHandler implements InvocationHandler {
        private static final Object[] EMPTY_ARGS = new Object[0];

        private Host host;

        private String objectId;
        private Map<String, Promise<ResultData>> waiting = new HashMap<>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (args == null) {
                args = EMPTY_ARGS;
            }

            if (this.host.isAlive) {
                String invocationId = UUID.randomUUID().toString();
                Class<?> returnType = method.getReturnType();

                Promise<Object> resultPromise = new Promise<>();
                Promise<ResultData> resultHandler = new Promise<>();

                resultHandler.then((data) -> {
                    try {
                        this.waiting.remove(invocationId);

                        Object result;

                        if (data.getContent() == null) {
                            result = null;
                        } else if (data.isRegularResult()) {
                            result = Rson.DEFAULT.fromJson(data.getContent(), returnType);
                        } else {
                            String objId = data.getContent().getAsObject().getString("objectId");
                            String objInterface = data.getContent().getAsObject().getString("objectInterface");

                            Class<?> objClazz = Class.forName(objInterface);
                            ProxyHandler handler = new ProxyHandler();

                            handler.host = this.host;
                            handler.objectId = objId;

                            result = Proxy.newProxyInstance(
                                objClazz.getClassLoader(),
                                arr(objClazz),
                                handler
                            );
                        }

                        resultPromise.fulfill(result);
                    } catch (JsonParseException | ClassNotFoundException ex) {
                        resultPromise.error(ex);
                    }
                });
                resultHandler.except((err) -> {
                    this.waiting.remove(invocationId);
                    resultPromise.error(err);
                });

                this.waiting.put(invocationId, resultHandler);

                // Packet.
                {
                    String methodName = method.getName();
                    JsonArray arguments = Rson.DEFAULT.toJson(args).getAsArray();

                    this.host.sendPacket(
                        new IpcPacketInvoke()
                            .setInvocationId(invocationId)
                            .setArgs(arguments)
                            .setMethod(methodName)
                            .setObjectId(this.objectId)
                    );
                }

                if (returnType == Promise.class) {
                    return resultPromise;
                } else {
                    return resultPromise.await();
                }
            } else {
                throw new IllegalStateException("IPC channel was closed.");
            }
        }
    }

    private static List<String> getExec(String main) throws IOException {
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        String entry = System.getProperty("sun.java.command"); // Tested, present in OpenJDK and Oracle
        String classpath = System.getProperty("java.class.path");
        String javaHome = System.getProperty("java.home");

        String[] args = entry.split(" ");
        File entryFile = new File(args[0]);

        if (entryFile.exists()) { // If the entry is a file, not a main method.
            args[0] = '"' + entryFile.getCanonicalPath() + '"'; // Use raw file path.

            classpath += ":" + entryFile.getCanonicalPath();
        }

        List<String> result = new ArrayList<>();

        result.add(String.format("\"%s/bin/java\"", javaHome));
        result.addAll(jvmArgs);
        result.add("-cp");
        result.add('"' + classpath + '"');
        result.add(main);
//        result.addAll(Arrays.asList(execArgs));

        return result;
    }

    @SafeVarargs
    private static <T> T[] arr(T... a) {
        return a;
    }

}
