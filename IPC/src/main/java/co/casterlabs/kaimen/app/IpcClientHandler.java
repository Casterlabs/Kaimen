package co.casterlabs.kaimen.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import javax.swing.JOptionPane;

import co.casterlabs.kaimen.app.IpcPacket.IpcPacketType;
import co.casterlabs.kaimen.app.IpcPacketPrint.PrintChannel;
import co.casterlabs.kaimen.util.threading.AsyncTask;
import co.casterlabs.kaimen.util.threading.MainThread;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonObject;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;
import xyz.e3ndr.fastloggingframework.logging.StringUtil;

public class IpcClientHandler {
    private static final PrintStream nativeOut = System.out;
    private static final InputStream nativeIn = System.in;

    private static IpcObject mainIpcObject; // So it doesn't get garbage collected.

    private static final FastLogger LOGGER = new FastLogger();

    static {
        LOGGER.setCurrentLevel(LogLevel.NONE); // Stop it.
    }

    public static void main(String[] unused) {
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true); // Enable assertions.

        overrideIO();

        MainThread.park(() -> {
            try (Scanner in = new Scanner(nativeIn)) {
                while (true) {
                    String line = in.nextLine();

                    new AsyncTask(() -> {
                        try {
                            IpcPacket packet = IpcPacketType.parsePacket(
                                Rson.DEFAULT.fromJson(line, JsonObject.class)
                            );

                            LOGGER.debug("Received packet:\n%s", line);

                            processPacket(packet);
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(null, StringUtil.getExceptionStack(e));
                            FastLogger.logStatic(LogLevel.SEVERE, "An error occured whilst processing packet: %s\n%s", line, e);
                        }
                    });
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, StringUtil.getExceptionStack(e));
            }
        });
    }

    private static void sendPacket(IpcPacket packet) {
        try {
            String packetStr = Rson.DEFAULT
                .toJson(packet)
                .toString();

            if (!(packet instanceof IpcPacketPrint)) {
                // Would cause an infinite loop.
                LOGGER.debug("Sent packet:\n%s", packetStr);
            }

            nativeOut.write(packetStr.getBytes());
            nativeOut.print('\n');
            nativeOut.flush();
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
                    resultPacket.setResult(
                        IpcObject
                            .getObject(invokePacket.getObjectId())
                            .invoke(invokePacket.getMethod(), invokePacket.getArgs())
                    );
                } catch (Throwable t) {
                    String error = StringUtil.getExceptionStack(t);

                    resultPacket.setError(error);
                }

                sendPacket(resultPacket);
                break;
            }

            default:
                break;
        }
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
                new IpcOutputStream(PrintChannel.STDOUT),
                true
            )
        );

        System.setErr(
            new PrintStream(
                new IpcOutputStream(PrintChannel.STDERR),
                true
            )
        );
    }

}
