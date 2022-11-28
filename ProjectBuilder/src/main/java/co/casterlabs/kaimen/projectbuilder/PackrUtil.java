package co.casterlabs.kaimen.projectbuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.badlogicgames.packr.PackrConfig;

import co.casterlabs.commons.functional.tuples.Pair;
import co.casterlabs.commons.platform.Arch;
import co.casterlabs.commons.platform.OSDistribution;

public class PackrUtil {
    public static final Map<Pair<OSDistribution, Arch>, PackrConfig.Platform> PLATFORM_MAPPING;

    static {
        Map<Pair<OSDistribution, Arch>, PackrConfig.Platform> mapping = new HashMap<>();

        mapping.put(new Pair<>(OSDistribution.WINDOWS_NT, Arch.X86_64), PackrConfig.Platform.Windows64);
        mapping.put(new Pair<>(OSDistribution.LINUX, Arch.X86_64), PackrConfig.Platform.Linux64);
        mapping.put(new Pair<>(OSDistribution.MACOS, Arch.X86_64), PackrConfig.Platform.MacOS);

        PLATFORM_MAPPING = Collections.unmodifiableMap(mapping);
    }

}
