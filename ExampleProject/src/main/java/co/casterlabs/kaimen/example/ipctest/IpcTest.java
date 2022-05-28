package co.casterlabs.kaimen.example.ipctest;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JOptionPane;

import co.casterlabs.kaimen.app.App;
import co.casterlabs.kaimen.app.App.Appearance;
import co.casterlabs.kaimen.app.App.PowerManagementHint;
import co.casterlabs.kaimen.app.AppBootstrap;
import co.casterlabs.kaimen.app.AppEntry;
import co.casterlabs.kaimen.app.IpcHostHandler;
import xyz.e3ndr.fastloggingframework.FastLoggingFramework;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class IpcTest {

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        AppBootstrap.main(args);
    }

    @AppEntry
    public static void entry() throws Throwable {
        FastLoggingFramework.setDefaultLevel(LogLevel.DEBUG);

        // Setup the app
        App.setName("IPC Test Application");
        App.setAppearance(Appearance.FOLLOW_SYSTEM);
        App.setPowermanagementHint(PowerManagementHint.HIGH_PERFORMANCE);

        JOptionPane.showMessageDialog(null, "Click OK to start the demo.");

        TestInterface test = IpcHostHandler.startInstance(TestImpl.class);

        test.showTestAlert();
        test.showTestAlert();
        test.showTestAlert();
        test.showTestAlert();
    }

}
