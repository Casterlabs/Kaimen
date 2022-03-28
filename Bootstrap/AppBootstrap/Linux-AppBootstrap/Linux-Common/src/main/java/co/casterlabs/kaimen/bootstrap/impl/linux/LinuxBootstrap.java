package co.casterlabs.kaimen.bootstrap.impl.linux;

import java.awt.Window;
import java.net.URL;
import java.util.Collections;

import javax.swing.ImageIcon;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.kaimen.app.App;
import co.casterlabs.kaimen.webview.Webview;
import co.casterlabs.kaimen.webview.impl.cef.CefWebview;
import lombok.NonNull;

public class LinuxBootstrap extends App {

    @Override
    protected void setName0(@NonNull String name) {
        for (Webview wv : Webview.getActiveWebviews()) {
            ((CefWebview) wv).updateTitle();
        }
    }

    @Override
    protected void setAppearance0(@NonNull Appearance appearance) {
        // TODO ugh.
    }

    @Override
    protected void setIcon0(@Nullable URL url) {
        for (Window window : Window.getWindows()) {
            if (url == null) {
                window.setIconImages(Collections.emptyList());
            } else {
                window.setIconImage(new ImageIcon(url).getImage());
            }
        }

    }

}
