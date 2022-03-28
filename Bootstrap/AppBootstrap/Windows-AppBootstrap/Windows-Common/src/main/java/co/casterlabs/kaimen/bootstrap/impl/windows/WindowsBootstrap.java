package co.casterlabs.kaimen.bootstrap.impl.windows;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.Collections;

import javax.swing.ImageIcon;

import org.jetbrains.annotations.Nullable;

import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.BOOLByReference;
import com.sun.jna.platform.win32.WinDef.HWND;

import co.casterlabs.kaimen.app.App;
import co.casterlabs.kaimen.webview.Webview;
import co.casterlabs.kaimen.webview.impl.cef.CefWebview;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class WindowsBootstrap extends App {

    public WindowsBootstrap() {
        Toolkit.getDefaultToolkit().addAWTEventListener(
            (event) -> {
                switch (event.getID()) {
                    case WindowEvent.WINDOW_OPENED:
                        Window source = (Window) event.getSource();
                        setWindowAppearance(source, App.getAppearance());
                        return;
                }
            },
            AWTEvent.WINDOW_EVENT_MASK
        );
    }

    @Override
    protected void setName0(@NonNull String name) {
        for (Webview wv : Webview.getActiveWebviews()) {
            ((CefWebview) wv).updateTitle();
        }
    }

    @Override
    protected void setAppearance0(@NonNull Appearance appearance) {
        for (Window window : Window.getWindows()) {
            if (window.isDisplayable()) {
                setWindowAppearance(window, appearance);
            }
        }
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

    private static void setWindowAppearance(Window window, @NonNull Appearance appearance) {
        // References:
        // https://docs.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmsetwindowattribute
        // https://winscp.net/forum/viewtopic.php?t=30088
        // https://gist.github.com/rossy/ebd83ba8f22339ce25ef68bfc007dfd2
        //
        // This is the code that we're mimicking (in c):
        /*
        DwmSetWindowAttribute(
            hwnd, 
            DWMWA_USE_IMMERSIVE_DARK_MODE,
            &(BOOL) { TRUE }, 
            sizeof(BOOL)
         );
         */

        HWND hwnd = DWM.getHWND(window);
        BOOLByReference pvAttribute = new BOOLByReference(new BOOL(appearance.isDark()));

        DWM.INSTANCE.DwmSetWindowAttribute(
            hwnd,
            DWM.DWMWA_USE_IMMERSIVE_DARK_MODE,
            pvAttribute,
            BOOL.SIZE
        );

        DWM.INSTANCE.DwmSetWindowAttribute(
            hwnd,
            DWM.DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1,
            pvAttribute,
            BOOL.SIZE
        );

        FastLogger.logStatic(
            LogLevel.DEBUG,
            "Set IMMERSIVE_DARK_MODE and USE_IMMERSIVE_DARK_MODE_BEFORE_20H1 to %b.",
            appearance.isDark()
        );

        // Tricks Windows into repainting the window.
        if (window.isVisible()) {
            window.setVisible(false);
            window.setVisible(true);
        }
    }

}
