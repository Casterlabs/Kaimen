package co.casterlabs.kaimen.app;

import java.lang.reflect.InvocationTargetException;

import co.casterlabs.kaimen.threading.MainThread;
import co.casterlabs.kaimen.webview.Webview;
import co.casterlabs.kaimen.webview.WebviewFactory;
import co.casterlabs.kaimen.webview.WebviewLifeCycleListener;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class Test {

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        MainThread.park(() -> {
            try {
                WebviewFactory factory = WebviewFactory.get();

                factory.setAppName("Example App");
                factory.setDarkMode(false);

                Webview webview = factory.produce();

                webview.initialize(new WebviewLifeCycleListener() {
                    @Override
                    public void onNavigate(String url) {
                        factory.setIconURL(
                            // There's a bug in the Google favicon api, it only really supports http://
                            // urls.
                            "https://t1.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&size=256&url=" + url.replace("https://", "http://")
                        );
                        FastLogger.logStatic("Navigated to: %s", url);
                    }
                }, null, false, false);

                webview.open("https://duckduckgo.com"); // It has a light theme by default
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
