package co.casterlabs.kaimen.ipc.client;

import static co.casterlabs.kaimen.ipc.KaimenIPC.PING_INTERVAL;
import static co.casterlabs.kaimen.ipc.KaimenIPC.PING_PACKET;
import static co.casterlabs.kaimen.ipc.KaimenIPC.PING_TIMEOUT;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Base64;
import java.util.Scanner;

import co.casterlabs.commons.async.AsyncTask;
import co.casterlabs.commons.async.queue.ThreadQueue;
import co.casterlabs.commons.ipc.IpcConnection;
import co.casterlabs.commons.ipc.packets.IpcPacket;
import co.casterlabs.commons.ipc.packets.IpcPacket.IpcPacketType;
import co.casterlabs.kaimen.ipc.packet.KaimenIpcPacket;
import co.casterlabs.kaimen.ipc.packet.KaimenIpcPrintPacket;
import co.casterlabs.kaimen.ipc.packet.KaimenIpcPrintPacket.PrintChannel;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonObject;
import lombok.AllArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.StringUtil;

public class IpcClientEntryPoint {
    private static final ThreadQueue dispatchThread = new ThreadQueue();
    private static final InputStream nativeIn = System.in;
    private static final PrintStream nativeOut = System.out;
    static final PrintStream nativeErr = System.err; // We use stderr for raw byte messages.

    private static long lastPing = System.currentTimeMillis();

    private static IpcClientHandler handler;

    final static WrappedIpcConnection connection = new WrappedIpcConnection();

    @SuppressWarnings({
            "deprecation",
            "unchecked"
    })
    public static void main(String[] args) throws Exception {
        // Read packets from stdin.
        AsyncTask.createNonDaemon(() -> {
            try (Scanner in = new Scanner(nativeIn)) {
                while (true) {
                    String line = in.nextLine();
                    JsonObject json = Rson.DEFAULT.fromJson(line, JsonObject.class);

                    IpcPacket packet = IpcPacketType.get(json);
                    connection.handlePacket(packet);
                }
            } catch (Exception e) {
                showMessage(StringUtil.getExceptionStack(e));
                return;
            }
        });

        // Ping thread.
        AsyncTask.createNonDaemon(() -> {
            try {
                while (true) {
                    Thread.sleep(PING_INTERVAL);

                    // Check and make sure we didn't timeout.
                    if (System.currentTimeMillis() > lastPing + PING_TIMEOUT) {
                        showMessage("PING FAILED");
                        return;
                    }

                    // Send a ping.
                    connection.sendMessage(PING_PACKET);
                }
            } catch (Exception e) {
                showMessage(StringUtil.getExceptionStack(e));
                return;
            }
        });

        // Override IO
        System.setOut(new PrintStream(new IpcOutputStream(PrintChannel.STDOUT), true));
        System.setErr(new PrintStream(new IpcOutputStream(PrintChannel.STDERR), true));
        System.setIn(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new UnsupportedOperationException("You cannot read System.in from an IPC child process.");
            }
        }); // NOOP

        String clientHandlerClassName = args[0];
        Class<? extends IpcClientHandler> clientHandlerClazz = (Class<? extends IpcClientHandler>) Class.forName(clientHandlerClassName);

        handler = clientHandlerClazz.newInstance();
    }

    static class WrappedIpcConnection extends IpcConnection {

        protected void handlePacket(IpcPacket packet) {
            this.receive(packet);
        }

        @Override
        protected void handleMessage(Object message) {
            if (message instanceof KaimenIpcPacket) {
                KaimenIpcPacket packet = (KaimenIpcPacket) message;

                switch (packet.getType()) {
                    case PING:
                        lastPing = System.currentTimeMillis();
                        break;

                    // Unused on this side.
                    case PRINT:
                        break;
                }
            } else {
                dispatchThread.submitTask(() -> {
                    handler.handleMessage(message);
                });
            }
        }

        @Override
        protected void send(IpcPacket packet) {
            nativeOut.println(Rson.DEFAULT.toJsonString(packet));
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

            String base64 = Base64.getEncoder().encodeToString(content);
            connection.sendMessage(new KaimenIpcPrintPacket(base64, this.printChannel));
        }
    }

    static void showMessage(String message) {
//        JOptionPane.showMessageDialog(null, message);
        System.exit(1);
    }

}
