package co.casterlabs.kaimen.projectbuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.badlogicgames.packr.Packr;
import com.badlogicgames.packr.PackrConfig;

import co.casterlabs.commons.platform.ArchFamily;
import co.casterlabs.commons.platform.OSDistribution;
import co.casterlabs.commons.platform.Platform;
import co.casterlabs.kaimen.webview.WebviewRenderer;
import co.casterlabs.rakurai.io.IOUtil;
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
    private OSDistribution targetOS;

    @Option(names = {
            "-arch",
            "--targetArch"
    }, description = "The target architecture to compile for", required = true)
    private ArchFamily targetArch;

    @Option(names = {
            "-archWord",
            "--targetArchWord"
    }, description = "The target architecture word size (32/64) to compile for", required = true)
    private int targetArchWordSize;

    @Option(names = {
            "-wi",
            "--webviewImplementation"
    }, description = "The webview implementation to use")
    private WebviewRenderer webviewImplementation; // NULL = Auto

    @Option(names = {
            "-kv",
            "--kaimenVersion"
    }, description = "The version of Kaimen to bundle", required = true)
    private String kaimenVersion;

    @Option(names = {
            "-cp",
            "--classpath"
    }, description = "The resources to be included on the classpath (e.g your app's .jar file)", required = true)
    private List<String> classPath;

    @Option(names = {
            "-res",
            "--resource"
    }, description = "The resources to be included next to your app's executable")
    private List<File> resources;

    @Option(names = {
            "-vm",
            "--vm-args"
    }, description = "The vmargs to start the application with (without the leading dash)")
    private List<String> vmArgs = new ArrayList<>();

    @Option(names = {
            "-jv",
            "--javaVersion"
    }, description = "The version of Java to bundle")
    private JavaVersion javaVersion = JavaVersion.JAVA11;

    @Option(names = {
            "-id",
            "--bundleId"
    }, description = "The bundle identifier to be used on MacOS")
    private String bundleIdentifier;

    @Option(names = {
            "-n",
            "--appName"
    }, description = "The name of the target executable (e.g my-application)", required = true)
    private String appName;

    @Option(names = {
            "-v",
            "--appVersion"
    }, description = "The version of application")
    private String appVersion = "0.0.0";

    @Option(names = {
            "-i",
            "--appIcon"
    }, description = "The icon of the application")
    private File appIcon;

    public static void main(String[] args) {
        new CommandLine(new ProjectBuilder()).execute(args);
    }

    @Override
    public void run() {
        if (this.debug) {
            FastLoggingFramework.setDefaultLevel(LogLevel.DEBUG);
        }

        if (this.webviewImplementation == null) {

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

        if (this.targetOS == OSDistribution.MACOS) {
            this.vmArgs.add("XstartOnFirstThread");
            config.iconResource = this.appIcon;
            outputDir = new File(outputDir, this.appName + ".app");
        }

        config.platform = PackrUtil.PLATFORM_MAPPING.get(this.targetOS.target + '-' + this.targetArch.getArchTarget(this.targetArchWordSize));
        config.jdk = this.javaVersion.getDownloadUrl(this.targetOS, this.targetArch.getArchTarget(this.targetArchWordSize));
        config.executable = this.appName;
        config.jrePath = "jre";
        config.classpath = this.classPath;
        config.mainClass = "co.casterlabs.kaimen.app.AppBootstrap";
        config.vmArgs = this.vmArgs;
        config.useZgcIfSupportedOs = false;
        config.resources = this.resources;
        config.outDir = outputDir;
        config.bundleIdentifier = this.bundleIdentifier;
        config.verbose = this.debug;

        try {
            File bootstrap = MavenUtil.getKaimenBootstrap(
                this.targetOS,
                this.targetArch.getArchTarget(this.targetArchWordSize),
                this.kaimenVersion
            );

            this.classPath.add(bootstrap.getCanonicalPath());
        } catch (Exception e) {
            FastLogger.logStatic(LogLevel.SEVERE, "Build prep failed :(");
            FastLogger.logException(e);
            return;
        }

        if (this.webviewImplementation != null) {
            try {
                File webview = MavenUtil.getWebviewBootstrap(
                    this.targetOS,
                    this.targetArch.getArchTarget(this.targetArchWordSize),
                    this.kaimenVersion,
                    this.webviewImplementation
                );

                this.classPath.add(webview.getCanonicalPath());
            } catch (Exception e) {
                FastLogger.logStatic(LogLevel.SEVERE, "Build prep failed :(");
                FastLogger.logException(e);
                return;
            }
        }

        try {
            new Packr().pack(config);

            this.doCompletionTasks(outputDir);

            FastLogger.logStatic("You heard Packr, build complete!");
            System.exit(0);
        } catch (Exception e) {
            FastLogger.logStatic(LogLevel.SEVERE, "Build failed :(");
            FastLogger.logException(e);
        }
    }

    private void doCompletionTasks(File outputDir) throws IOException, InterruptedException {
        if (this.targetOS == OSDistribution.MACOS) {
            File infoPlist = new File(outputDir, "/Contents/Info.plist");

            String addition = IOUtil.readString(ProjectBuilder.class.getResourceAsStream("/add_Info.plist"));
            String contents = Files.readString(infoPlist.toPath());

            contents.replace("</dict>\n</plist>", addition);

            Files.writeString(infoPlist.toPath(), contents);
        } else if (this.targetOS == OSDistribution.WINDOWS_NT) {
            if (Platform.osDistribution == OSDistribution.WINDOWS_NT && new File("rcedit-x64.exe").exists()) {
                File executable = new File(outputDir, this.appName + ".exe");

                if (this.appIcon != null) {
                    RceditUtil.setIconFile(executable, this.appIcon);
                }

                if (this.appVersion != null) {
                    RceditUtil.setVersionString(executable, this.appVersion);
                }
            }
        }
    }

    private void doPreflightChecks() {
        if (!PackrUtil.PLATFORM_MAPPING.containsKey(this.targetOS.target + '-' + this.targetArch.getArchTarget(this.targetArchWordSize))) {
            FastLogger.logStatic(LogLevel.SEVERE, "Unfortunately, Packr does not support %s-%s.", this.targetOS.target, this.targetArch.getArchTarget(this.targetArchWordSize));
            System.exit(1);
        }

        if (this.targetOS == OSDistribution.MACOS) {
            if (Platform.osDistribution == OSDistribution.WINDOWS_NT) {
                FastLogger.logStatic(LogLevel.SEVERE, "You cannot target MacOS when building on windows.");
                System.exit(1);
            }

            assert this.bundleIdentifier != null : "You must specify a bundle identifier when building for MacOS";
            assert this.appIcon != null : "You must specify an app icon when building for MacOS";
            assert this.appIcon.getName().endsWith(".icns") : "App icon must be an .icns file when building for MacOS";
        }

        if (this.targetOS == OSDistribution.LINUX) {
            if (Platform.osDistribution == OSDistribution.WINDOWS_NT) {
                FastLogger.logStatic(LogLevel.SEVERE, "You cannot target Linux when building on windows.");
                System.exit(1);
            }
        }

        if (this.targetOS == OSDistribution.WINDOWS_NT) {
            if (!new File("rcedit-x64.exe").exists()) {
                FastLogger.logStatic(LogLevel.WARNING, "In order for the setting of appIcon and/or appVersion to work you will need to download the 64 bit version of rcedit from here: https://github.com/electron/rcedit/releases/latest");
            }

            if (((this.appIcon != null) || (this.appVersion != null)) &&
                (Platform.osDistribution != OSDistribution.WINDOWS_NT)) {
                FastLogger.logStatic(LogLevel.WARNING, "Setting appIcon and/or appVersion when building for Windows but not building on Windows has no effect.");
            }

            if (this.appIcon != null) {
                assert this.appIcon.getName().endsWith(".ico") : "App icon must be an .ico file when building for Windows";
            }
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
