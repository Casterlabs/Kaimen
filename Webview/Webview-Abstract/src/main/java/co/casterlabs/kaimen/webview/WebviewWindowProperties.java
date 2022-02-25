package co.casterlabs.kaimen.webview;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;

@Value
@AllArgsConstructor
public class WebviewWindowProperties {
    private @With boolean focusable;
    private @With boolean alwaysOnTop;

    public WebviewWindowProperties() {
        this(true, false);
    }

}
