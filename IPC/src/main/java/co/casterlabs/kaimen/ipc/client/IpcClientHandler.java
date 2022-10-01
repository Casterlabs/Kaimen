package co.casterlabs.kaimen.ipc.client;

import xyz.e3ndr.fastloggingframework.logging.StringUtil;

public abstract class IpcClientHandler {

    public final void sendMessage(Object message) {
        IpcClientEntryPoint.connection.sendMessage(message);
    }

    public final void sendByteMessage(int type, byte[] message) {
        try {
            int length = message.length;

            IpcClientEntryPoint.nativeErr.write(length);  // 1) Write the length.
            IpcClientEntryPoint.nativeErr.write(type);    // 2) Write the user provided type.
            IpcClientEntryPoint.nativeErr.write(message); // 3) Write the message.
            IpcClientEntryPoint.nativeErr.write(null);    // 4) Finally, write a null, so we can double check the read was good.
            IpcClientEntryPoint.nativeErr.flush();
        } catch (Exception e) {
            IpcClientEntryPoint.showMessage(StringUtil.getExceptionStack(e));
            System.exit(1);
        }
    }

    public abstract void handleMessage(Object message);

}
