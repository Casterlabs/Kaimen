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

    public static void create(boolean enableOsr, String webviewToken) {
        try {
            CefAppBuilder builder = new CefAppBuilder();

            builder.addJcefArgs("--disable-http-cache", "--disable-web-security");
            builder.setInstallDir(bundleDirectory);

            CefSettings settings = builder.getCefSettings();

            settings.windowless_rendering_enabled = enableOsr;
            settings.log_severity = LogSeverity.LOGSEVERITY_DISABLE;
            settings.user_agent_product = String.format("Chromium; Just A Kaimen App (%s)", webviewToken);

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
