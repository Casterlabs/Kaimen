package co.casterlabs.kaimen.projectbuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.badlogicgames.packr.Packr;
import com.badlogicgames.packr.PackrConfig;

import co.casterlabs.kaimen.util.platform.Arch;
import co.casterlabs.kaimen.util.platform.OperatingSystem;
import co.casterlabs.kaimen.util.platform.Platform;
import co.casterlabs.rakurai.io.IOUtil;
import kotlin.Pair;
import lombok.ToString;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import xyz.e3ndr.fastloggingframework.FastLoggingFramework;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@ToString
@Command(name = "build", mixinStandardHelpOptions = true, version = "1.0.0", description = "Starts building")
public class ProjectBuilder implements Runnable {

    @Option(names = {
            "-d",
            "--debug"
    }, description = "Enables debug logging")
    private boolean debug = false;

    @Option(names = {
            "-os",
            "--targetOS"
    }, description = "The target operating system to compile for", required = true)
    private OperatingSystem targetOS;

    @Option(names = {
            "-arch",
            "--targetArch"
    }, description = "The target architecture to compile for", required = true)
    private Arch targetArch;

    @Option(names = {
            "-cp",
            "--classpath"
    }, description = "The resources to be included on the classpath (e.g your app's .jar file)", required = true)
    private List<String> classPath;

    @Option(names = {
            "-vm",
            "--vm-args"
    }, description = "The vmargs to start the application with (without the leading dash)")
    private List<String> vmArgs = new ArrayList<>();

    @Option(names = {
            "-res",
            "--resource"
    }, description = "The resources to be included next to your app's executable")
    private List<File> resources;

    @Option(names = {
            "-n",
            "--name"
    }, description = "The name of the target executable (e.g my_application)", required = true)
    private String executableName;

    @Option(names = {
            "-m",
            "--mainClass"
    }, description = "The mainclass to be executed", required = true)
    private String mainClass;

    @Option(names = {
            "-id",
            "--bundleId"
    }, description = "The bundle identifier to be used on MacOS")
    private String bundleIdentifier;

    @Option(names = {
            "-bi",
            "--bundleIcon"
    }, description = "The bundle icon to be used on MacOS (.icns file)")
    private File bundleIcon;

    @Option(names = {
            "-jv",
            "--javaVersion"
    }, description = "The version of Java to bundle")
    private JavaVersion javaVersion = JavaVersion.JAVA11;

    public static void main(String[] args) {
        new CommandLine(new ProjectBuilder()).execute(args);
    }

    @Override
    public void run() {
        if (this.debug) {
            FastLoggingFramework.setDefaultLevel(LogLevel.DEBUG);
        }

        this.doPreflightChecks();

        File outputDir = new File(String.format("./dist/%s-%s", this.targetOS, this.targetArch));

        if (outputDir.exists()) {
            // Clean the output dir.
            for (File f : outputDir.listFiles()) {
                deleteFolder(f);
            }
        } else {
            outputDir.mkdirs();
        }

        FastLogger.logStatic("Building for %s:%s", this.targetOS, this.targetArch);
        FastLogger.logStatic("Output dir: %s", outputDir);

        PackrConfig config = new PackrConfig();

        if (this.targetOS == OperatingSystem.MACOSX) {
            this.vmArgs.add("XstartOnFirstThread");
        }

        config.platform = PackrUtil.PLATFORM_MAPPING.get(new Pair<>(this.targetOS, this.targetArch));
        config.jdk = this.javaVersion.getDownloadUrl(this.targetOS, this.targetArch);
        config.executable = this.executableName;
        config.jrePath = "jre";
        config.classpath = this.classPath;
        // removelibs
        config.mainClass = this.mainClass;
        config.vmArgs = Collections.emptyList();
        config.useZgcIfSupportedOs = false;
        config.resources = this.resources;
        // minimizejre
        config.outDir = outputDir;
        // cachejre
        config.iconResource = this.bundleIcon;
        config.bundleIdentifier = this.bundleIdentifier;
        config.verbose = this.debug;

        try {
            new Packr().pack(config);

            this.doCompletionTasks(outputDir);

            FastLogger.logStatic("You heard Packr, build complete!");
        } catch (Exception e) {
            FastLogger.logStatic(LogLevel.SEVERE, "Build failed :(");
            FastLogger.logException(e);
        }
    }

    private void doCompletionTasks(File outputDir) throws IOException {
        if (this.targetOS == OperatingSystem.MACOSX) {
            File appBundle = outputDir.listFiles()[0];
            File infoPlist = new File(appBundle, "/Contents/Info.plist");

            String addition = IOUtil.readString(ProjectBuilder.class.getResource("add_Info.plist").openStream());
            String contents = Files.readString(infoPlist.toPath());

            contents.replace("</dict>\n</plist>", addition);

            Files.writeString(infoPlist.toPath(), contents);
        }
    }

    private void doPreflightChecks() {
        if (this.targetOS == OperatingSystem.MACOSX) {
            if (Platform.os == OperatingSystem.WINDOWS) {
                FastLogger.logStatic(LogLevel.SEVERE, "You cannot target MacOS when building on windows.");
                System.exit(1);
            }

            assert this.bundleIdentifier != null : "You must specify a bundle identifier when building for MacOS";
            assert this.bundleIcon != null : "You must specify a bundle icon when building for MacOS";
        }

        if ((this.targetOS == OperatingSystem.LINUX) && (Platform.os == OperatingSystem.WINDOWS)) {
            FastLogger.logStatic(LogLevel.SEVERE, "You cannot target Linux when building on windows.");
            System.exit(1);
        }

        if (!PackrUtil.PLATFORM_MAPPING.containsKey(new Pair<>(this.targetOS, this.targetArch))) {
            FastLogger.logStatic(LogLevel.SEVERE, "Unfortunately, Packr does not support %s:%s.", this.targetOS, this.targetArch);
            System.exit(1);
        }
    }

    private static void deleteFolder(File folder) {
        File[] files = folder.listFiles();

        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }

        folder.delete();
    }

}
