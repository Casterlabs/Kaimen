package co.casterlabs.kaimen.example.ipctest;

import javax.swing.JOptionPane;

import co.casterlabs.kaimen.app.IpcObject;

public class TestImpl extends IpcObject implements TestInterface {

    @Override
    public void showTestAlert() {
        JOptionPane.showMessageDialog(null, "Test from IPC!", "Kaimen IPC Test", JOptionPane.PLAIN_MESSAGE);
    }

    @Override
    protected Class<?> getInterfaceClass0() {
        return TestInterface.class;
    }

}
