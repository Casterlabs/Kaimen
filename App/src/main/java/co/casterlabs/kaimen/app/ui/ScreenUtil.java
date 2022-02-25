package co.casterlabs.kaimen.app.ui;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import co.casterlabs.kaimen.util.EventProvider;
import co.casterlabs.kaimen.util.threading.AsyncTask;
import co.casterlabs.kaimen.util.threading.MainThreadPromise;

public class ScreenUtil {
    public static final EventProvider<Rectangle> sizeEvent = new EventProvider<>();

    private static Rectangle fullSize;
    private static boolean initialized = false;

    public static Rectangle getFullSize() {
        if (!initialized) {
            // We can't use the mainthread in a static initializer (causes deadlock)
            fullSize = calculateSize();

            new AsyncTask(() -> {
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

        return fullSize;
    }

    private static Rectangle calculateSize() {
        return new MainThreadPromise<>(() -> {
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
