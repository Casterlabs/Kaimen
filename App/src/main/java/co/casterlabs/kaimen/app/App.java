package co.casterlabs.kaimen.app;

import java.net.URL;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.kaimen.app.platform.Platform;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;

@SuppressWarnings("deprecation")
public abstract class App {
    private static App instance;

    private static @Getter String appName = "Kaimen Application";
    private static @Getter boolean usingDarkTheme = false;
    private static @Getter @Nullable URL iconURL;

    static {
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true); // Enable assertions.

        try {
            switch (Platform.os) {
                case LINUX:
                    instance = (App) Class.forName("co.casterlabs.kaimen.bootstrap.impl.linux.LinuxBootstrap").newInstance();
                    break;

                case MACOSX:
                    instance = (App) Class.forName("co.casterlabs.kaimen.bootstrap.impl.macos.MacOSBootstrap").newInstance();
                    break;

                case WINDOWS:
                    instance = (App) Class.forName("co.casterlabs.kaimen.bootstrap.impl.windows.WindowsBootstrap").newInstance();
                    break;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find bootstrap:", e);
        }
    }

    public static void setAppName(@NonNull String name) {
        assert !name.isEmpty() : "App name must not be empty.";
        appName = name;
        instance.setAppName0(name);
    }

    public static void setTheme(boolean useDark) {
        usingDarkTheme = useDark;
        instance.setTheme0(useDark);
    }

    public static void setAppIcon(@Nullable URL url) {
        iconURL = url;
        instance.setAppIcon0(url);
    }

    @SneakyThrows
    public static void setAppIconURL(@Nullable String url) {
        if (url == null) {
            setAppIcon(null);
        } else {
            setAppIcon(new URL(url));
        }
    }

    /* Impl */

    protected abstract void setAppName0(@NonNull String name);

    protected abstract void setTheme0(boolean useDark);

    protected abstract void setAppIcon0(@Nullable URL url);

}
