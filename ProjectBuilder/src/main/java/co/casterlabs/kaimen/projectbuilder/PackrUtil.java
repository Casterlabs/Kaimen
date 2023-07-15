package co.casterlabs.kaimen.projectbuilder;

import java.util.Map;

import com.badlogicgames.packr.PackrConfig;

import co.casterlabs.commons.functional.tuples.Pair;
import co.casterlabs.commons.platform.ArchFamily;
import co.casterlabs.commons.platform.OSDistribution;

public class PackrUtil {
    public static final Map<Pair<OSDistribution, String>, PackrConfig.Platform> PLATFORM_MAPPING = Map.of(
        new Pair<>(OSDistribution.WINDOWS_NT, ArchFamily.X86.getArchTarget(64)), PackrConfig.Platform.Windows64,
        new Pair<>(OSDistribution.LINUX, ArchFamily.X86.getArchTarget(64)), PackrConfig.Platform.Linux64,
        new Pair<>(OSDistribution.MACOS, ArchFamily.X86.getArchTarget(64)), PackrConfig.Platform.MacOS
    );

}
