package co.casterlabs.kaimen.app;

import java.awt.Window;
import java.net.URL;

import org.jetbrains.annotations.Nullable;

import com.jthemedetecor.OsThemeDetector;

import co.casterlabs.commons.async.queue.ThreadQueue;
import co.casterlabs.commons.events.EventProvider;
import co.casterlabs.kaimen.util.reflection.FieldMutationListener;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import xyz.e3ndr.reflectionlib.ReflectionLib;

public abstract class App {
    private static App instance;
    private static EventProvider<AppEvent, Void> eventProvider = new EventProvider();

    private static @Getter String name = "Kaimen Application";
    private static @Getter Appearance appearance;
    private static @Getter @Nullable URL iconURL;
    private static @Getter @NonNull PowerManagementHint powerManagementHint = PowerManagementHint.BALANCED;

    private static OsThemeDetector themeDetector = OsThemeDetector.getDetector();

    @Getter
    private static ThreadQueue mainThread;

    @Getter
    private static String[] args;

    static void init(String[] args, App instance, ThreadQueue mainThread) {
        App.args = args;
        App.instance = instance;
        App.mainThread = mainThread;

        try {
            ReflectionLib.setStaticValue(Class.forName("co.casterlabs.kaimen.webview.Webview"), "mainThread", mainThread);
        } catch (Exception ignored) {}

        setAppearance(Appearance.FOLLOW_SYSTEM);
        setPowermanagementHint(PowerManagementHint.BALANCED);

        themeDetector.registerListener((ignored) -> {
            if (appearance == Appearance.FOLLOW_SYSTEM) {
                instance.setAppearance0(appearance);
                eventProvider.fireEvent(AppEvent.APPEARANCE_CHANGE, null);
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

    protected void shakeWindowProperties0(Window window) {
        // Used internally as-needed.
    }

    protected ThreadQueue.Impl getMainThreadImpl() {
        // Used internally as-needed.
        return null;
    }

    /* Events */

    public static synchronized int on(@NonNull AppEvent type, @NonNull Runnable listener) {
        return eventProvider.on(type, listener);
    }

    public static synchronized void off(int id) {
        eventProvider.off(id);
    }

    /* Public */

    @Deprecated
    public static void shakeWindowProperties(@NonNull Window window) {
        instance.shakeWindowProperties0(window);
    }

    @SneakyThrows
    public static void setPowermanagementHint(@NonNull PowerManagementHint hint) {
        powerManagementHint = hint;
        eventProvider.fireEvent(AppEvent.POWER_HINT_CHANGE, null);

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
        instance.setIcon0(url);
        eventProvider.fireEvent(AppEvent.ICON_CHANGE, null);
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
