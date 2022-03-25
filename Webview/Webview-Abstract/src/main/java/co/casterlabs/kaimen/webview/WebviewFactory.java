package co.casterlabs.kaimen.webview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import co.casterlabs.kaimen.util.functional.Producer;
import co.casterlabs.kaimen.util.platform.Arch;
import co.casterlabs.kaimen.util.platform.OperatingSystem;
import co.casterlabs.kaimen.util.platform.Platform;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;
import xyz.e3ndr.reflectionlib.ReflectionLib;

public abstract class WebviewFactory implements Producer<Webview> {
    private static WebviewFactory FACTORY;

    static {
        try {
            List<WebviewFactory> factories = new ArrayList<>();

            try {
                factories.add(
                    ReflectionLib.getStaticValue(
                        Class.forName("co.casterlabs.kaimen.webview.impl.cef.CefWebview"),
                        "FACTORY"
                    )
                );
            } catch (Exception ignored) {}

            try {
                factories.add(
                    ReflectionLib.getStaticValue(
                        Class.forName("co.casterlabs.kaimen.webview.impl.webkit.WkWebview"),
                        "FACTORY"
                    )
                );
            } catch (Exception ignored) {}

            try {
                factories.add(
                    ReflectionLib.getStaticValue(
                        Class.forName("co.casterlabs.kaimen.webview.impl.webviewproject.WvWebview"),
                        "FACTORY"
                    )
                );
            } catch (Exception ignored) {}

            if (FACTORY == null) {
                for (WebviewFactory factory : factories) {
                    try {
                        if (factory.supportsPlatform()) {
                            FACTORY = factory;
                            break;
                        }
                    } catch (Throwable ignored) {}
                }

                if (FACTORY == null) {
                    FastLogger.logStatic(LogLevel.SEVERE, "Could not find any webviews. Is your project configured correctly?");
                    throw new IllegalStateException("Cannot find any webviews. Is your project configured correctly?");
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

    public boolean supportsPlatform() {
        return this
            .getSupportMap()
            .getOrDefault(Platform.os, Collections.emptyList())
            .contains(Platform.arch);
    }

    public abstract WebviewRenderer getRendererType();

    public abstract Map<OperatingSystem, List<Arch>> getSupportMap();

    public boolean supportsOSR() {
        return true;
    }

    public boolean supportsTransparency() {
        return true;
    }

}
