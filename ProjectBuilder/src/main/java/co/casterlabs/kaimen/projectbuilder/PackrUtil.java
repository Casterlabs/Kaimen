package co.casterlabs.kaimen.projectbuilder;

import java.util.Map;

import com.badlogicgames.packr.PackrConfig;

import co.casterlabs.commons.platform.ArchFamily;
import co.casterlabs.commons.platform.OSDistribution;

public class PackrUtil {
    public static final Map<String, PackrConfig.Platform> PLATFORM_MAPPING = Map.of(
        OSDistribution.WINDOWS_NT.target + '-' + ArchFamily.X86.getArchTarget(64), PackrConfig.Platform.Windows64,
        OSDistribution.LINUX.target + '-' + ArchFamily.X86.getArchTarget(64), PackrConfig.Platform.Linux64,
        OSDistribution.MACOS.target + '-' + ArchFamily.X86.getArchTarget(64), PackrConfig.Platform.MacOS
    );

}
