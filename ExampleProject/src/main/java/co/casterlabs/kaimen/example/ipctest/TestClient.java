package co.casterlabs.kaimen.example.ipctest;

import co.casterlabs.kaimen.ipc.client.IpcClientHandler;

public class TestClient extends IpcClientHandler {

    public TestClient() {
        this.sendMessage("Hello host!");
    }

    @Override
    public void handleMessage(Object message) {
        System.out.printf("Message from host: %s\n", message);
    }

}
