package co.casterlabs.kaimen.projectbuilder;

import java.io.File;
import java.io.IOException;

import co.casterlabs.kaimen.util.platform.Arch;
import co.casterlabs.kaimen.util.platform.OperatingSystem;
import co.casterlabs.kaimen.util.platform.Platform;
import xyz.e3ndr.fastloggingframework.FastLoggingFramework;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class MavenUtil {

    public static File getKaimenBootstrap(OperatingSystem os, Arch arch, String version) throws InterruptedException, IOException {
        final String group = "co.casterlabs.kaimen";
        String artifact = String.format("%s-%s", os, arch);

        artifact = artifact.substring(0, 1).toUpperCase() + artifact.substring(1); // Just some silly string manipulation.

        downloadDependency(group, artifact, version);

        return new File(String.format("./dist/tmp/%s-%s-shaded.jar", artifact, version));
    }

    public static void downloadDependency(String group, String artifact, String version) throws InterruptedException, IOException {
        FastLogger.logStatic("Downloading dependency: %s:%s:%s", group, artifact, version);

        execute(
            "mvn",
            "dependency:get",
            "-DremoteRepositories=https://repo.maven.apache.org/maven2,https://jitpack.io",
            "-DgroupId=" + group,
            "-DartifactId=" + artifact,
            "-Dversion=" + version,
            "-Dclassifier=shaded",
            "-Dtransitive=false"
        );

        FastLogger.logStatic("Copying dependency: %s:%s:%s", group, artifact, version);

        // Copy the result.
        execute(
            "mvn",
            "dependency:copy",
            String.format("-Dartifact=%s:%s:%s:jar:shaded", group, artifact, version),
            "-DoutputDirectory=./dist/tmp"
        );

        FastLogger.logStatic("Finished downloading dependency: %s:%s:%s", group, artifact, version);
    }

    private static void execute(String... cmd) throws InterruptedException, IOException {
        String[] trueCommand;

        if (Platform.os == OperatingSystem.WINDOWS) {
            trueCommand = new String[] {
                    "cmd",
                    "/c",
                    String.join(" ", cmd)
            };
        } else {
            trueCommand = cmd; // Unix doesn't have these issues.
        }

        ProcessBuilder pb = new ProcessBuilder()
            .command(trueCommand);

        if (FastLoggingFramework.getDefaultLevel() == LogLevel.DEBUG) {
            pb.inheritIO();
        }

        pb.start().waitFor();
    }

}
