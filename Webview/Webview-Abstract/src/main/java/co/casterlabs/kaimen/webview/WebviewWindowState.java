package co.casterlabs.kaimen.webview;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import co.casterlabs.kaimen.webview.bridge.JavascriptObject;
import co.casterlabs.kaimen.webview.bridge.JavascriptValue;
import co.casterlabs.rakurai.json.annotating.JsonClass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;

@Data
@JsonClass(exposeAll = true)
@EqualsAndHashCode(callSuper = false)
public class WebviewWindowState extends JavascriptObject {
    private @JavascriptValue(allowSet = false, watchForMutate = true) boolean maximized = false;
    private @JavascriptValue(allowSet = false, watchForMutate = true) boolean hasFocus;

    private @JavascriptValue(allowSet = false, watchForMutate = true) int x;
    private @JavascriptValue(allowSet = false, watchForMutate = true) int y;
    private @JavascriptValue(allowSet = false, watchForMutate = true) int width = 800;
    private @JavascriptValue(allowSet = false, watchForMutate = true) int height = 600;

    @SneakyThrows
    public WebviewWindowState() {
        // Setup Defaults.
        // Needs to be done on the main thread.
        Webview.mainThread.submitTaskAndWait(() -> {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

            int monitorWidth = gd.getDisplayMode().getWidth();
            int monitorHeight = gd.getDisplayMode().getHeight();

            this.x = (monitorWidth - this.width) / 2;
            this.y = (monitorHeight - this.height) / 2;
        });
    }

    /* Override as needed */

    public int getMinWidth() {
        return 800;
    }

    public int getMinHeight() {
        return 600;
    }

}
