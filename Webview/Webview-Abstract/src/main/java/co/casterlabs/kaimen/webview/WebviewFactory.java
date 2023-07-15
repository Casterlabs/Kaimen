package co.casterlabs.kaimen.webview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import co.casterlabs.commons.platform.OSDistribution;
import co.casterlabs.commons.platform.Platform;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;
import xyz.e3ndr.reflectionlib.ReflectionLib;

public abstract class WebviewFactory implements Supplier<Webview> {
    private static Map<WebviewRenderer, WebviewFactory> factories = new HashMap<>();

    static {
        List<WebviewFactory> includedFactories = new ArrayList<>();

        try {
            includedFactories.add(
                ReflectionLib.getStaticValue(
                    Class.forName("co.casterlabs.kaimen.webview.impl.cef.CefWebview"),
                    "FACTORY"
                )
            );
        } catch (Exception ignored) {}

        try {
            includedFactories.add(
                ReflectionLib.getStaticValue(
                    Class.forName("co.casterlabs.kaimen.webview.impl.webkit.WkWebview"),
                    "FACTORY"
                )
            );
        } catch (Exception ignored) {}

        try {
            includedFactories.add(
                ReflectionLib.getStaticValue(
                    Class.forName("co.casterlabs.kaimen.webview.impl.webviewdev.WvWebview"),
                    "FACTORY"
                )
            );
        } catch (Exception ignored) {}

        FastLogger.logStatic(LogLevel.DEBUG, "Found factories: %s", includedFactories);

        for (WebviewFactory factory : includedFactories) {
            FastLogger.logStatic(LogLevel.DEBUG, "%s factory supports: %s", factory.getRendererType(), factory.getSupportMap());

            if (factory.supportsPlatform()) {
                factories.put(factory.getRendererType(), factory);
            }
        }

        if (factories.isEmpty()) {
            FastLogger.logStatic(LogLevel.SEVERE, "Could not find any webviews. Is your project configured correctly?");
            throw new IllegalStateException("Cannot find any webviews. Is your project configured correctly?");
        }
    }

    public static WebviewFactory getFactory(WebviewRenderer... orderOfPreference) {
        if (orderOfPreference.length == 0) {
            orderOfPreference = WebviewRenderer.values();
        }

        for (WebviewRenderer renderer : orderOfPreference) {
            WebviewFactory factory = factories.get(renderer);
            if (factory != null) {
                return factory;
            }
        }

        throw new IllegalStateException("Could not find a suitable webview factory that satisfies preference: " + Arrays.toString(orderOfPreference));
    }

    public boolean supportsPlatform() {
        return this
            .getSupportMap()
            .getOrDefault(Platform.osDistribution, Collections.emptyList())
            .contains(Platform.archFamily.getArchTarget(Platform.wordSize));
    }

    public abstract WebviewRenderer getRendererType();

    public abstract Map<OSDistribution, List<String>> getSupportMap();

    public boolean supportsOSR() {
        return true;
    }

    public boolean supportsTransparency() {
        return true;
    }

}
