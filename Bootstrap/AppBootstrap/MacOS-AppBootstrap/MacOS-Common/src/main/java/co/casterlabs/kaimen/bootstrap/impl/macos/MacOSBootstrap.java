package co.casterlabs.kaimen.bootstrap.impl.macos;

import java.net.URL;

import org.eclipse.swt.internal.cocoa.OS;
import org.eclipse.swt.widgets.Display;
import org.jetbrains.annotations.Nullable;

import co.casterlabs.commons.async.queue.ThreadQueue;
import co.casterlabs.commons.async.queue.ThreadQueue.Impl;
import co.casterlabs.kaimen.app.App;
import co.casterlabs.kaimen.webview.Webview;
import co.casterlabs.kaimen.webview.impl.webkit.WkWebview;
import lombok.NonNull;

public class MacOSBootstrap extends App {
    private Display display;

    public MacOSBootstrap() {
        this.display = new Display();
    }

    @Override
    protected void setName0(@NonNull String name) {
        Display.setAppName(name);

        for (Webview wv : Webview.getActiveWebviews()) {
            if (wv instanceof WkWebview) {
                ((WkWebview) wv).updateTitle();
            }
        }
    }

    @Override
    protected void setAppearance0(@NonNull Appearance appearance) {
        App.getMainThread().submitTask(() -> {
            OS.setTheme(appearance.isDark());
        });
    }

    @Override
    protected Impl getMainThreadImpl() {
        return new ThreadQueue.Impl() {
            @Override
            public Thread getThread() {
                return display.getThread();
            }

            @Override
            public void submitTask(Runnable run) {
                display.asyncExec(run);
            }
        };
    }

    @Override
    protected void setIcon0(@Nullable URL url) {
        for (Webview wv : Webview.getActiveWebviews()) {
            if (wv instanceof WkWebview) {
                ((WkWebview) wv).changeImage(url);
            }
        }
    }

}
