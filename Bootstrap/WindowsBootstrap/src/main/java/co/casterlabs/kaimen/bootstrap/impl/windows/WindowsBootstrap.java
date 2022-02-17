package co.casterlabs.kaimen.bootstrap.impl.windows;

import java.awt.Window;
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

    @Override
    protected void setAppName0(@NonNull String name) {
        for (Webview wv : Webview.getActiveWebviews()) {
            ((CefWebview) wv).updateTitle();
        }
    }

    @Override
    protected void setTheme0(boolean useDark) {
        for (Window window : Window.getWindows()) {
            window.setVisible(true); // Make it visible, if not already.

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
            BOOLByReference pvAttribute = new BOOLByReference(new BOOL(useDark));

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
                "Set IMMERSIVE_DARK_MODE and DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1 to %b.",
                useDark
            );

            // Tricks Windows into repainting the window.
            window.setVisible(false);
            window.setVisible(true);
        }
    }

    @Override
    protected void setAppIcon0(@Nullable URL url) {
        for (Window window : Window.getWindows()) {
            if (url == null) {
                window.setIconImages(Collections.emptyList());
            } else {
                window.setIconImage(new ImageIcon(url).getImage());
            }
        }

    }

}
