package co.casterlabs.kaimen.example;

import co.casterlabs.commons.platform.Platform;
import co.casterlabs.kaimen.app.App;
import co.casterlabs.kaimen.app.App.Appearance;
import co.casterlabs.kaimen.app.App.PowerManagementHint;
import co.casterlabs.kaimen.app.AppBootstrap;
import co.casterlabs.kaimen.app.AppEntry;
import co.casterlabs.kaimen.app.ui.UIServer;
import co.casterlabs.kaimen.webview.Webview;
import co.casterlabs.kaimen.webview.WebviewFactory;
import co.casterlabs.kaimen.webview.WebviewLifeCycleListener;
import co.casterlabs.kaimen.webview.WebviewRenderer;
import co.casterlabs.kaimen.webview.WebviewWindowProperties;
import co.casterlabs.kaimen.webview.bridge.JavascriptFunction;
import co.casterlabs.kaimen.webview.bridge.JavascriptObject;
import co.casterlabs.kaimen.webview.bridge.JavascriptValue;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rhs.protocol.StandardHttpStatus;
import co.casterlabs.rhs.server.HttpResponse;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.FastLoggingFramework;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class Test {

    public static void main(String[] args) throws Exception {
        AppBootstrap.main(args);
    }

    @AppEntry
    public static void entry() throws Exception {
        FastLoggingFramework.setDefaultLevel(LogLevel.TRACE);

        // Setup the app
        App.setName("Example Application");
        App.setAppearance(Appearance.FOLLOW_SYSTEM);
        App.setPowermanagementHint(PowerManagementHint.HIGH_PERFORMANCE);

        // UI Server
        @SuppressWarnings("resource")
        UIServer uiServer = new UIServer()
            .setIgnorePassword(true)
            .setHandler((session) -> {
                return HttpResponse.newFixedLengthResponse(
                    StandardHttpStatus.OK,
                    "<!DOCTYPE html>"
                        + "<html style=\"background-color: transparent;\">"
                        + "<body style=\"text-align: center; font-family: BlinkMacSystemFont, -apple-system, 'Segoe UI', Ubuntu, Cantarell, 'Fira Sans', 'Droid Sans', 'Helvetica Neue', Helvetica, Arial, sans-serif;\">"
                        + "<br />"
                        + "<br />"
                        + "Example App"
                        + "<br />"
                        + "<br />"
                        + "<a href='https://google.com'>Open Google</a> &nbsp;&nbsp; "
                        + "<a href='https://casterlabs.co'>Open Casterlabs</a>"
                        + "<p>x: <span id='x'></span> y: <span id='y'></span></p>"
                        + "<script>"
                        + "function onBridgeInit() {"
                        + "const xElem = document.querySelector('#x');"
                        + "const yElem = document.querySelector('#y');"
                        + "windowState.mutate('x', (x) => xElem.innerText = x);"
                        + "windowState.mutate('y', (y) => yElem.innerText = y);"
                        + "}"
                        + "</script>"
                        + "</body>"
                        + "</html"
                );
            })
            .start();

        WebviewFactory factory = WebviewFactory.getFactory(WebviewRenderer.WEBKIT, WebviewRenderer.WEBVIEW_DEV, WebviewRenderer.CHROMIUM_EMBEDDED_FRAMEWORK);

        // Log some stuff
        FastLogger.logStatic("Running on: %s (%s)", Platform.osDistribution, Platform.arch);
        FastLogger.logStatic("Using: %s", factory.getRendererType());
        FastLogger.logStatic("System Appearance: %s", App.getSystemAppearance());
        FastLogger.logStatic("UI Server port (it's ephemeral): %d", uiServer.getPort());

        // Setup the webview
        Webview webview = factory.get();

        webview.initialize(new WebviewLifeCycleListener() {
            @Override
            public void onNavigate(String url) {
                try {
                    App.setIconURL(
                        // There's a bug in the Google favicon api, it only really supports http://
                        // urls.
                        "https://t1.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&size=256&url=" + url.replace("https://", "http://")
                    );
                    FastLogger.logStatic("Navigated to: %s", url);
                } catch (Exception ignored) {}
            }

            @SneakyThrows
            @Override
            public void onCloseRequested() {
                uiServer.close();
                System.exit(0);
            }
        }, null, false, false);

        webview.setProperties(
            new WebviewWindowProperties()
                .withAlwaysOnTop(false)
                .withFocusable(true)
        );

        webview
            .getBridge()
            .defineObject("test", new JavascriptObject() {
                @JavascriptValue
                private int twelve = 12;

                @JavascriptFunction
                public JsonElement echo(JsonElement e) {
                    return e;
                }

                private JavascriptObject system = new JavascriptObject() {

                    @JavascriptFunction
                    public long nanoTime() {
                        return System.nanoTime();
                    }

                    @JavascriptFunction
                    public void testThrow() {
                        throw new IllegalStateException("Test throw.");
                    }

                };

            });

        webview.open(uiServer.getLocalAddress());
    }

}
