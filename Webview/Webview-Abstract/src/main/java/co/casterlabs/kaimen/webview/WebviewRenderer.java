package co.casterlabs.kaimen.webview;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum WebviewRenderer {
    WEBKIT("WebKit"),
    CHROMIUM_EMBEDDED_FRAMEWORK("CEF"),
    WEBVIEW_PROJECT("WV");

    private String name;

    @Override
    public String toString() {
        return this.name;
    }

}
