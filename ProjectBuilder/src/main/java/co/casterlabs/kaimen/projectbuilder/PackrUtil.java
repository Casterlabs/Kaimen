package co.casterlabs.kaimen.projectbuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.badlogicgames.packr.PackrConfig;

import co.casterlabs.kaimen.util.platform.Arch;
import co.casterlabs.kaimen.util.platform.OperatingSystem;
import kotlin.Pair;

public class PackrUtil {
    public static final Map<Pair<OperatingSystem, Arch>, PackrConfig.Platform> PLATFORM_MAPPING;

    static {
        Map<Pair<OperatingSystem, Arch>, PackrConfig.Platform> mapping = new HashMap<>();

        mapping.put(new Pair<>(OperatingSystem.WINDOWS, Arch.AMD64), PackrConfig.Platform.Windows64);
        mapping.put(new Pair<>(OperatingSystem.LINUX, Arch.AMD64), PackrConfig.Platform.Linux64);
        mapping.put(new Pair<>(OperatingSystem.MACOSX, Arch.AMD64), PackrConfig.Platform.MacOS);

        PLATFORM_MAPPING = Collections.unmodifiableMap(mapping);
    }

}
