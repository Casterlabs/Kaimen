package co.casterlabs.kaimen.webview;

import co.casterlabs.kaimen.util.functional.Producer;
import co.casterlabs.kaimen.util.platform.Platform;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;
import xyz.e3ndr.reflectionlib.ReflectionLib;

public abstract class WebviewFactory implements Producer<Webview> {
    private static WebviewFactory FACTORY;

    static {
        try {
            Class<?> wkWebview = null;
            Class<?> cefWebview = null;

            try {
                wkWebview = Class.forName("co.casterlabs.kaimen.webview.impl.webkit.WkWebview");
            } catch (Exception ignored) {}

            try {
                cefWebview = Class.forName("co.casterlabs.kaimen.webview.impl.cef.CefWebview");
            } catch (Exception ignored) {}

            switch (Platform.os) {
                case MACOSX:
                    if (wkWebview != null) {
                        FACTORY = ReflectionLib.getStaticValue(wkWebview, "FACTORY");
                        break;
                    } else {
                        break;
                    }

                case LINUX:
                case WINDOWS:
                    if (cefWebview != null) {
                        FACTORY = ReflectionLib.getStaticValue(cefWebview, "FACTORY");
                        break;
                    } else {
                        break;
                    }
            }

            if (FACTORY == null) {
                if ((wkWebview == null) && (cefWebview == null)) {
                    FastLogger.logStatic(LogLevel.SEVERE, "Could not find any webviews. Is your project configured correctly?");
                    throw new RuntimeException();
                } else {
                    FastLogger.logStatic(LogLevel.WARNING, "Could not find appropriate webview for %s. Is your project configured correctly? Using fallback.", Platform.os);

                    if (wkWebview != null) {
                        FACTORY = ReflectionLib.getStaticValue(wkWebview, "FACTORY");
                    } else {
                        FACTORY = ReflectionLib.getStaticValue(cefWebview, "FACTORY");
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static WebviewFactory get() {
        assert FACTORY != null : "Could not find a suitable webview factory.";
        return FACTORY;
    }

    public abstract WebviewRenderer getRendererType();

}
