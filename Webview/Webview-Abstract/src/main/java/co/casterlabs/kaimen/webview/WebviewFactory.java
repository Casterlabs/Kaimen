package co.casterlabs.kaimen.webview;

import java.net.URL;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.kaimen.util.Producer;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import xyz.e3ndr.reflectionlib.ReflectionLib;

public abstract class WebviewFactory implements Producer<Webview> {
    private static WebviewFactory FACTORY;

    private static @Getter @Nullable String appName = null;
    private static @Getter @Nullable URL currentIcon = null;
    private static @Getter boolean isDarkMode = false;

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

    public void setDarkMode(boolean enabled) {
        isDarkMode = enabled;
        this.setDarkMode0(isDarkMode);
    }

    public void setIcon(@NonNull URL icon) {
        currentIcon = icon;
        this.setIcon0(icon);
    }

    public void setAppName(@Nullable String name) {
        appName = name;
        this.setAppName0(name);
    }

    @SneakyThrows
    public void setIconURL(String url) {
        this.setIcon(new URL(url));
    }

    public abstract boolean useNuclearOption();

    protected abstract void setAppName0(@Nullable String name);

    protected abstract void setIcon0(@NonNull URL icon);

    protected abstract void setDarkMode0(boolean isDarkMode);

}
