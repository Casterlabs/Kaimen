package co.casterlabs.kaimen.app;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.jthemedetecor.OsThemeDetector;

import co.casterlabs.kaimen.util.platform.Platform;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;

@SuppressWarnings("deprecation")
public abstract class App {
    private static App instance;

    private static @Getter String name = "Kaimen Application";
    private static @Getter Appearance appearance;
    private static @Getter @Nullable URL iconURL;

    private static OsThemeDetector themeDetector = OsThemeDetector.getDetector();

    private static Set<Consumer<Appearance>> systemThemeChangeListeners = new HashSet<>();

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

            setAppearance(Appearance.FOLLOW_SYSTEM);

            themeDetector.registerListener((ignored) -> {
                if (appearance == Appearance.FOLLOW_SYSTEM) {
                    instance.setAppearance0(appearance);

                    Appearance a = getSystemAppearance();

                    systemThemeChangeListeners.forEach((l) -> l.accept(a));
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to find bootstrap:", e);
        }
    }

    /* Impl */

    protected abstract void setName0(@NonNull String name);

    protected abstract void setAppearance0(@NonNull Appearance appearance);

    protected abstract void setIcon0(@Nullable URL url);

    protected @NonNull Appearance getSystemAppearance0() {
        return themeDetector.isDark() ? Appearance.DARK : Appearance.LIGHT;
    }

    /* Public */

    public static void addSystemThemeChangeListener(@NonNull Consumer<Appearance> listener) {
        systemThemeChangeListeners.add(listener);
    }

    public static void removeSystemThemeChangeListener(@NonNull Consumer<Appearance> listener) {
        systemThemeChangeListeners.remove(listener);
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

    public static @NonNull Appearance getSystemAppearance() {
        return instance.getSystemAppearance0();
    }

    public static enum Appearance {
        DARK,
        LIGHT,
        FOLLOW_SYSTEM;

        public boolean isDark() {
            if (this == FOLLOW_SYSTEM) {
                return getSystemAppearance() == DARK;
            } else {
                return this == DARK;
            }
        }
    }

}
