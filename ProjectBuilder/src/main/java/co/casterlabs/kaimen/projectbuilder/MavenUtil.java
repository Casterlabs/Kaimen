package co.casterlabs.kaimen.projectbuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import co.casterlabs.commons.platform.OSDistribution;
import co.casterlabs.kaimen.webview.WebviewRenderer;
import co.casterlabs.rakurai.io.IOUtil;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class MavenUtil {

    public static File getKaimenBootstrap(OSDistribution os, String archTarget, String version) throws InterruptedException, IOException {
        final String group = "co.casterlabs.kaimen";

        String osStr = os.target;
        osStr = osStr.substring(0, 1).toUpperCase() + osStr.substring(1); // Just some silly string manipulation.

        String artifact = String.format("%s-%s", osStr, archTarget);

        return downloadDependency(group, artifact, version);
    }

    public static File getWebviewBootstrap(OSDistribution os, String archTarget, String version, WebviewRenderer webviewImplementation) throws InterruptedException, IOException {
        final String group = "co.casterlabs.kaimen";

        String osStr = os.target;
        osStr = osStr.substring(0, 1).toUpperCase() + osStr.substring(1); // Just some silly string manipulation.

        String artifact = String.format("%s-%s-%s", webviewImplementation, osStr, archTarget);

        return downloadDependency(group, artifact, version);
    }

    private static File downloadDependency(String group, String artifact, String version) throws InterruptedException, IOException {
        FastLogger.logStatic("Downloading dependency: %s:%s:%s", group, artifact, version);

        String url = String.format("https://jitpack.io/%s/%s/%s/%s-%s-shaded.jar", group.replace('.', '/'), artifact, version, artifact, version);
        File target = new File(String.format("./dist/tmp/%s-%s-shaded.jar", artifact, version));

        if (!target.exists()) {
            target.getParentFile().mkdirs();
            target.createNewFile();

            try (InputStream in = new URL(url).openStream()) {
                try (OutputStream out = new FileOutputStream(target)) {
                    IOUtil.writeInputStreamToOutputStream(in, out);
                }
            }
        }

        return target;
    }

}
