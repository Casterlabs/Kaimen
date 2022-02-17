package co.casterlabs.kaimen.bootstrap.impl.macos;

import java.net.URL;

import org.eclipse.swt.internal.cocoa.OS;
import org.eclipse.swt.widgets.Display;
import org.jetbrains.annotations.Nullable;

import co.casterlabs.kaimen.app.App;
import co.casterlabs.kaimen.threading.MainThread;
import co.casterlabs.kaimen.webview.Webview;
import co.casterlabs.kaimen.webview.impl.webkit.WkWebview;
import lombok.NonNull;

public class MacOSBootstrap extends App {

    public MacOSBootstrap() {
        MainThread.submitTask(() -> {
            // Init the display for the main thread.
            // We need to create atleast one display for OS.setTheme() to work.
            Display.getDefault();
        });
    }

    @Override
    protected void setAppName0(@NonNull String name) {
        Display.setAppName(name);

        for (Webview wv : Webview.getActiveWebviews()) {
            ((WkWebview) wv).updateTitle();
        }
    }

    @Override
    protected void setTheme0(boolean useDark) {
        MainThread.submitTask(() -> {
            OS.setTheme(useDark);
        });
    }

    @Override
    protected void setAppIcon0(@Nullable URL url) {
        for (Webview wv : Webview.getActiveWebviews()) {
            ((WkWebview) wv).changeImage(url);
        }
    }

}
