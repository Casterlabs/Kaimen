package co.casterlabs.kaimen.webview;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum WebviewRenderer {
    WEBKIT("WebKit"),
    CHROMIUM_EMBEDDED_FRAMEWORK("CEF"),

    ;

    private String name;

    @Override
    public String toString() {
        return this.name;
    }

}
