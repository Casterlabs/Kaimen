package co.casterlabs.kaimen.projectbuilder;

import java.io.File;
import java.io.IOException;

import xyz.e3ndr.fastloggingframework.FastLoggingFramework;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class RceditUtil {

    public static void setVersionString(File executable, String version) throws InterruptedException, IOException {
        execute("rcedit-x64", executable.getCanonicalPath(), "--set-file-version", version);
        execute("rcedit-x64", executable.getCanonicalPath(), "--set-product-version", version);
    }

    public static void setIconFile(File executable, File icon) throws InterruptedException, IOException {
        execute("rcedit-x64", executable.getCanonicalPath(), "--set-icon", icon.getCanonicalPath());
    }

    private static void execute(String... cmd) throws InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder()
            .command(cmd);

        if (FastLoggingFramework.getDefaultLevel() == LogLevel.DEBUG) {
            pb.inheritIO();
        }

        pb.start().waitFor();
    }

}
