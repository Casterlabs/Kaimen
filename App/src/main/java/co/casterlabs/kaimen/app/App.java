package co.casterlabs.kaimen.app;

import java.net.URL;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.kaimen.platform.Platform;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;

@SuppressWarnings("deprecation")
public abstract class App {
    private static App instance;

    private static @Getter String name = "Kaimen Application";
    private static @Getter Appearance appearance;
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

            setAppearance(Appearance.LIGHT); // Default.
        } catch (Exception e) {
            throw new RuntimeException("Failed to find bootstrap:", e);
        }
    }

    public static void setName(@NonNull String newName) {
        assert !newName.isEmpty() : "App name must not be empty.";
        name = newName;
        instance.setName0(name);
    }

    public static void setAppearance(@NonNull Appearance newAppearance) {
        appearance = newAppearance;
        instance.setAppearance0(appearance);
    }

    public static void setIcon(@Nullable URL url) {
        iconURL = url;
        instance.setIcon0(url);
    }

    @SneakyThrows
    public static void setIconURL(@Nullable String url) {
        if (url == null) {
            setIcon(null);
        } else {
            setIcon(new URL(url));
        }
    }

    public static enum Appearance {
        DARK,
        LIGHT;

        public boolean isDark() {
            return this == DARK;
        }
    }

    /* Impl */

    protected abstract void setName0(@NonNull String name);

    protected abstract void setAppearance0(@NonNull Appearance appearance);

    protected abstract void setIcon0(@Nullable URL url);

}
