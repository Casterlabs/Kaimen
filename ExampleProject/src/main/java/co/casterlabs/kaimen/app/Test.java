package co.casterlabs.kaimen.app;

import java.lang.reflect.InvocationTargetException;

import co.casterlabs.kaimen.platform.Platform;
import co.casterlabs.kaimen.threading.MainThread;
import co.casterlabs.kaimen.webview.Webview;
import co.casterlabs.kaimen.webview.WebviewFactory;
import co.casterlabs.kaimen.webview.WebviewLifeCycleListener;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class Test {

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        MainThread.park(() -> {
            try {
                FastLogger.logStatic("Running on: %s (%s)", Platform.os, Platform.arch);

                App.setName("Example Application");
                App.setAppearance(App.Appearance.LIGHT);

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
