package co.casterlabs.kaimen.app;

import java.lang.reflect.InvocationTargetException;

import co.casterlabs.kaimen.app.App.Appearance;
import co.casterlabs.kaimen.util.platform.Platform;
import co.casterlabs.kaimen.util.threading.MainThread;
import co.casterlabs.kaimen.webview.Webview;
import co.casterlabs.kaimen.webview.WebviewFactory;
import co.casterlabs.kaimen.webview.WebviewLifeCycleListener;
import xyz.e3ndr.fastloggingframework.FastLoggingFramework;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class Test {

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        MainThread.park(() -> {
            try {
                FastLoggingFramework.setDefaultLevel(LogLevel.DEBUG);

                FastLogger.logStatic("Running on: %s (%s)", Platform.os, Platform.arch);
                FastLogger.logStatic("System Appearance: %s", App.getSystemAppearance());

                App.setName("Example Application");
                App.setAppearance(Appearance.FOLLOW_SYSTEM);

                WebviewFactory factory = WebviewFactory.get();

                Webview webview = factory.produce();

                webview.initialize(new WebviewLifeCycleListener() {
                    @Override
                    public void onNavigate(String url) {
                        App.setIconURL(
                            // There's a bug in the Google favicon api, it only really supports http://
                            // urls.
                            "https://t1.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&size=256&url=" + url.replace("https://", "http://")
                        );
                        FastLogger.logStatic("Navigated to: %s", url);
                    }
                }, null, false, false);

                webview.open("https://google.com");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
