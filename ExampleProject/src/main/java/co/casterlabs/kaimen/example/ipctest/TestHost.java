package co.casterlabs.kaimen.example.ipctest;

import java.io.IOException;

import co.casterlabs.kaimen.ipc.host.IpcHostHandler;

public class TestHost extends IpcHostHandler {

    public TestHost() throws IOException {
        super(TestClient.class);
        this.sendMessage("Hello client!");
    }

    @Override
    public void handleMessage(Object message) {
        System.out.printf("Message from client: %s\n", message);
    }

    @Override
    public void handleByteMessage(int type, byte[] message) {}

    @Override
    public void onClose() {
        System.out.println("Client closed");
    }

}
