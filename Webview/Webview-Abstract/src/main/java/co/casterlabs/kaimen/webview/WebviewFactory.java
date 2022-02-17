package co.casterlabs.kaimen.webview;

import co.casterlabs.kaimen.util.Producer;
import xyz.e3ndr.reflectionlib.ReflectionLib;

public abstract class WebviewFactory implements Producer<Webview> {
    private static WebviewFactory FACTORY;

    static {
        try {
            FACTORY = ReflectionLib.getStaticValue(Class.forName("co.casterlabs.kaimen.webview.impl.webkit.WkWebview"), "FACTORY");
        } catch (Exception ignored) {}

        try {
            FACTORY = ReflectionLib.getStaticValue(Class.forName("co.casterlabs.kaimen.webview.impl.cef.CefWebview"), "FACTORY");
        } catch (Exception ignored) {}
    }

    public static WebviewFactory get() {
        assert FACTORY != null : "Could not find a suitable webview factory.";
        return FACTORY;
    }

    /**
     * This is used internally.
     */
    @Deprecated
    public abstract boolean useNuclearOption();

}
