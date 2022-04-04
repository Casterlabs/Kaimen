package co.casterlabs.kaimen.app;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import co.casterlabs.kaimen.app.IpcPacket.IpcPacketType;
import co.casterlabs.kaimen.app.IpcPacketPrint.PrintChannel;
import co.casterlabs.kaimen.util.threading.AsyncTask;
import co.casterlabs.kaimen.util.threading.Promise;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class IpcHostHandler {

    @SuppressWarnings("unchecked")
    public static <T extends IpcObject> T startInstance(@NonNull Class<T> interfaceClazz, @NonNull Class<? extends T> implementingClazz) throws InterruptedException, Throwable {
        Process process = new ProcessBuilder()
            .command(getExec("co.casterlabs.kaimen.app.IpcClientHandler", App.getArgs()))
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
                    process.getOutputStream().write(
                        Rson.DEFAULT
                            .toJson(packet)
                            .toString()
                            .getBytes()
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                    process.destroy();
                }
            }
        };

        host.sendPacket(
            new IpcPacketInit()
                .setTargetClass(implementingClazz.getCanonicalName())
        );

        new AsyncTask(() -> {
            try {
                process.waitFor();
            } catch (InterruptedException e) {}

            host.isAlive = false;
        });

        new AsyncTask(() -> {
            try (Scanner in = new Scanner(process.getInputStream())) {
                while (in.hasNext()) {
                    String line = in.nextLine();

                    new AsyncTask(() -> {
                        try {
                            IpcPacket packet = IpcPacketType.parsePacket(
                                Rson.DEFAULT.fromJson(line, JsonObject.class)
                            );

                            switch (packet.getType()) {
                                case CLIENT_READY: {
                                    IpcPacketClientReady clientReadyPacket = (IpcPacketClientReady) packet;

                                    handler.objectId = clientReadyPacket.getObjectId();
                                    startPromise.fulfill(null);
                                    break;
                                }

                                case INVOCATION_RESULT: {
                                    IpcPacketInvocationResult resultPacket = (IpcPacketInvocationResult) packet;
                                    Promise<JsonElement> promise = handler.waiting.get(resultPacket.getInvocationId());

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
                                    } else {
                                        System.err.write(printPacket.getBytes());
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
            new Class[] {
                    interfaceClazz
            },
            handler
        );
    }

    private static abstract class Host {
        private boolean isAlive = true;

        public abstract void sendPacket(IpcPacket packet);

    }

    private static class ProxyHandler implements InvocationHandler {
        private Host host;

        private String objectId;
        private Map<String, Promise<JsonElement>> waiting = new HashMap<>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (this.host.isAlive) {
                String invocationId = UUID.randomUUID().toString();
                Class<?> returnType = method.getReturnType();

                Promise<Object> resultPromise = new Promise<>();
                Promise<JsonElement> resultHandler = new Promise<>();

                resultHandler.then((e) -> {
                    try {
                        this.waiting.remove(invocationId);
                        resultPromise.fulfill(
                            Rson.DEFAULT.fromJson(e, returnType)
                        );
                    } catch (JsonParseException ex) {
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

    private static List<String> getExec(String main, String[] execArgs) throws IOException {
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
        result.addAll(Arrays.asList(execArgs));

        return result;
    }

}
