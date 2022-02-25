package co.casterlabs.kaimen.webview;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import co.casterlabs.kaimen.util.threading.MainThread;
import co.casterlabs.kaimen.webview.bridge.BridgeValue;
import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.rakurai.json.annotating.JsonExclude;
import lombok.Data;
import lombok.SneakyThrows;

@Data
@JsonClass(exposeAll = true)
public class WebviewWindowState {
    private boolean maximized = false;
    private boolean hasFocus;

    private int x;
    private int y;
    private int width = 800;
    private int height = 600;

    private @JsonExclude BridgeValue<WebviewWindowState> bridge = new BridgeValue<>("window", this);

    @SneakyThrows
    public WebviewWindowState() {
        // Setup Defaults.
        // Needs to be done on the main thread.
        MainThread.submitTaskAndWait(() -> {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

            int monitorWidth = gd.getDisplayMode().getWidth();
            int monitorHeight = gd.getDisplayMode().getHeight();

            this.x = (monitorWidth - this.width) / 2;
            this.y = (monitorHeight - this.height) / 2;
        });
    }

    public void update() {
        this.bridge.update();
    }

    /* Override as needed */

    public int getMinWidth() {
        return 800;
    }

    public int getMinHeight() {
        return 580;
    }

}
