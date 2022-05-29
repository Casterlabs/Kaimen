package co.casterlabs.kaimen.webview.impl.cef;

import java.io.File;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.CefSettings.LogSeverity;

import lombok.SneakyThrows;
import me.friwi.jcefmaven.CefAppBuilder;

public class CefUtil {
    public static final File bundleDirectory = new File("cef_bundle");

    public static void create(boolean enableOsr) {
        try {
            CefAppBuilder builder = new CefAppBuilder();

            builder.addJcefArgs("--disable-http-cache", "--disable-web-security");
            builder.setInstallDir(bundleDirectory);

            CefSettings settings = builder.getCefSettings();

            settings.background_color = settings.new ColorType(0, 0, 0, 0);
            settings.windowless_rendering_enabled = enableOsr;
            settings.log_severity = LogSeverity.LOGSEVERITY_DISABLE;

            // TODO figure out how to keep this up-to-date
//            settings.user_agent_product = String.format("Chrome/95.0.4638.69 Kaimen (%s)", webviewToken);

            builder.setProgressHandler(new CefDownloadProgressDialog());

            builder.build();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @SneakyThrows
    public static CefClient createCefClient() {
        return CefApp.getInstance().createClient();
    }

}
