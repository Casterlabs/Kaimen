package co.casterlabs.kaimen.app.ui;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import co.casterlabs.commons.async.AsyncTask;
import co.casterlabs.commons.events.SimpleEventProvider;
import co.casterlabs.kaimen.app.App;
import lombok.SneakyThrows;

public class ScreenUtil {
    public static final SimpleEventProvider<Rectangle> sizeEvent = new SimpleEventProvider<>();

    private static Rectangle fullSize;
    private static boolean initialized = false;

    static {
        AsyncTask.create(() -> {
            try {
                while (true) {
                    Rectangle size = calculateSize();

                    if (!size.equals(fullSize)) {
                        fullSize = size;
                        sizeEvent.fireEvent(size);
                    }

                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static Rectangle getFullSize() {
        if (!initialized) {
            // We can't use the main thread in a static initializer (causes deadlock)
            fullSize = calculateSize();
            initialized = true;
        }

        return fullSize;
    }

    @SneakyThrows
    private static Rectangle calculateSize() {
        return App.getMainThread().executeWithPromise(() -> {
            Rectangle result = new Rectangle();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

            for (GraphicsDevice gd : ge.getScreenDevices()) {
                for (GraphicsConfiguration graphicsConfiguration : gd.getConfigurations()) {
                    Rectangle.union(result, graphicsConfiguration.getBounds(), result);
                }
            }

            return result;
        }).await();
    }

}
