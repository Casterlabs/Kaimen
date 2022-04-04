package co.casterlabs.kaimen.app;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import co.casterlabs.kaimen.app.IpcPacket.IpcPacketType;
import co.casterlabs.kaimen.app.IpcPacketPrint.PrintChannel;
import co.casterlabs.kaimen.util.platform.Platform;
import co.casterlabs.kaimen.util.threading.AsyncTask;
import co.casterlabs.kaimen.util.threading.MainThread;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonObject;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;
import xyz.e3ndr.fastloggingframework.logging.LoggingUtil;

public class IpcClientHandler {
    private static final PrintStream nativeOut = System.out;
    private static final InputStream nativeIn = System.in;

    private static IpcObject mainIpcObject; // So it doesn't get garbage collected.

    @SuppressWarnings("deprecation")
    public static void main(String[] args /* These are always the args the host was started with */) {
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true); // Enable assertions.

        MainThread.park(() -> {
            // Init the framework
            try {
                final App instance;

                switch (Platform.os) {
                    case LINUX:
                        instance = (App) Class.forName("co.casterlabs.kaimen.bootstrap.impl.linux.LinuxBootstrap").newInstance();
                        break;

                    case MACOSX:
                        instance = (App) Class.forName("co.casterlabs.kaimen.bootstrap.impl.macos.MacOSBootstrap").newInstance();
                        break;

                    case WINDOWS:
                        instance = (App) Class.forName("co.casterlabs.kaimen.bootstrap.impl.windows.WindowsBootstrap").newInstance();
                        break;

                    default:
                        // Shut up compiler.
                        return;
                }

                App.init(args, instance);
                init();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to find bootstrap:", e);
            }
        });
    }

    private static void sendPacket(IpcPacket packet) {
        try {
            nativeOut.write(
                Rson.DEFAULT
                    .toJson(packet)
                    .toString()
                    .getBytes()
            );
        } catch (Exception e) {
            // An error occured, exit.
            System.exit(1);
        }
    }

    @SuppressWarnings("deprecation")
    @SneakyThrows
    private static void processPacket(IpcPacket packet) {
        switch (packet.getType()) {
            case INIT: {
                IpcPacketInit initPacket = (IpcPacketInit) packet;
                Class<?> clazz = Class.forName(initPacket.getTargetClass());

                mainIpcObject = (IpcObject) clazz.newInstance();

                sendPacket(
                    new IpcPacketClientReady()
                        .setObjectId(mainIpcObject.getId())
                );

                break;
            }

            case INVOKE: {
                IpcPacketInvoke invokePacket = (IpcPacketInvoke) packet;
                IpcPacketInvocationResult resultPacket = new IpcPacketInvocationResult()
                    .setInvocationId(invokePacket.getInvocationId());

                try {
                    JsonElement result = IpcObject
                        .getObject(invokePacket.getObjectId())
                        .invoke(invokePacket.getMethod(), invokePacket.getArgs());

                    resultPacket.setResult(result);
                } catch (Throwable t) {
                    String error = LoggingUtil.getExceptionStack(t);

                    resultPacket.setError(error);
                }

                sendPacket(resultPacket);
                break;
            }

            default:
                break;
        }
    }

    private static void init() {
        overrideIO();

        new AsyncTask(() -> {
            try (Scanner in = new Scanner(nativeIn)) {
                while (in.hasNext()) {
                    String line = in.nextLine();

                    new AsyncTask(() -> {
                        try {
                            IpcPacket packet = IpcPacketType.parsePacket(
                                Rson.DEFAULT.fromJson(line, JsonObject.class)
                            );

                            processPacket(packet);
                        } catch (Exception e) {
                            FastLogger.logStatic(LogLevel.SEVERE, "An error occured whilst processing packet: %s\n%s", line, e);
                        }
                    });
                }
            }
        });
    }

    @AllArgsConstructor
    private static class IpcOutputStream extends OutputStream {
        private PrintChannel printChannel;

        @Override
        public void write(int b) throws IOException {} // Never called.

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            byte[] content = new byte[len];
            System.arraycopy(b, off, content, 0, len);

            sendPacket(
                new IpcPacketPrint()
                    .setBytes(content)
                    .setChannel(this.printChannel)
            );
        }
    }

    private static void overrideIO() {
        System.setIn(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new UnsupportedOperationException("You cannot read System.in from an IPC child process.");
            }
        }); // NOOP

        System.setOut(
            new PrintStream(
                new BufferedOutputStream(
                    new IpcOutputStream(PrintChannel.STDOUT)
                )
            )
        );

        System.setErr(
            new PrintStream(
                new BufferedOutputStream(
                    new IpcOutputStream(PrintChannel.STDERR)
                )
            )
        );
    }

}
