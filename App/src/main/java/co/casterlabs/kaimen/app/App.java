package co.casterlabs.kaimen.app;

import java.net.URL;

import org.jetbrains.annotations.Nullable;

import com.jthemedetecor.OsThemeDetector;

import co.casterlabs.kaimen.util.EventProvider;
import co.casterlabs.kaimen.util.reflection.FieldMutationListener;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import xyz.e3ndr.reflectionlib.ReflectionLib;

public abstract class App {
    private static App instance;

    private static @Getter String name = "Kaimen Application";
    private static @Getter Appearance appearance;
    private static @Getter @Nullable URL iconURL;
    private static @Getter @NonNull PowerManagementHint powerManagementHint = PowerManagementHint.BALANCED;

    private static OsThemeDetector themeDetector = OsThemeDetector.getDetector();

    public static final EventProvider<Appearance> systemThemeChangeEvent = new EventProvider<>();
    public static final EventProvider<URL> appIconChangeEvent = new EventProvider<>();
    public static final EventProvider<PowerManagementHint> powerManagementHintChangeEvent = new EventProvider<>();

    @Getter
    private static String[] args;

    static void init(String[] args, App instance) {
        App.args = args;
        App.instance = instance;

        setAppearance(Appearance.FOLLOW_SYSTEM);
        setPowermanagementHint(PowerManagementHint.BALANCED);

        themeDetector.registerListener((ignored) -> {
            if (appearance == Appearance.FOLLOW_SYSTEM) {
                instance.setAppearance0(appearance);

                Appearance a = getSystemAppearance();

                systemThemeChangeEvent.fireEvent(a);
            }
        });
    }

    /* Impl */

    protected abstract void setName0(@NonNull String name);

    protected abstract void setAppearance0(@NonNull Appearance appearance);

    protected abstract void setIcon0(@Nullable URL url);

    protected @NonNull Appearance getSystemAppearance0() {
        return themeDetector.isDark() ? Appearance.DARK : Appearance.LIGHT;
    }

    /* Public */

    @SneakyThrows
    public static void setPowermanagementHint(@NonNull PowerManagementHint hint) {
        powerManagementHint = hint;
        powerManagementHintChangeEvent.fireEvent(hint);

        int fieldMutationListener_pollInterval = 0;

        // Set variables based off of the hint.
        switch (hint) {
            case POWER_SAVER:
                fieldMutationListener_pollInterval = 150;
                break;

            case BALANCED:
                fieldMutationListener_pollInterval = 100;
                break;

            case HIGH_PERFORMANCE:
                fieldMutationListener_pollInterval = 50;
                break;
        }

        ReflectionLib.setStaticValue(FieldMutationListener.class, "POLL_INTERVAL", fieldMutationListener_pollInterval);
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
        appIconChangeEvent.fireEvent(url);
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

    public static enum PowerManagementHint {
        POWER_SAVER,
        BALANCED,
        HIGH_PERFORMANCE
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
