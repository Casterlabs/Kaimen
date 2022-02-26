package co.casterlabs.kaimen.projectbuilder;

import java.util.HashMap;
import java.util.Map;

import co.casterlabs.kaimen.util.platform.Arch;
import co.casterlabs.kaimen.util.platform.OperatingSystem;
import co.casterlabs.rakurai.StringUtil;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum JavaVersion {
    JAVA11("jdk-11.0.13%2B8");

    private static final Map<OperatingSystem, String> OS_MAPPING = new HashMap<>();
    private static final Map<Arch, String> ARCH_MAPPING = new HashMap<>();

    private String versionString;

    static {
        OS_MAPPING.put(OperatingSystem.WINDOWS, "windows");
        OS_MAPPING.put(OperatingSystem.MACOSX, "mac");
        OS_MAPPING.put(OperatingSystem.LINUX, "linux");
        ARCH_MAPPING.put(Arch.AMD64, "x64");
        ARCH_MAPPING.put(Arch.X86, "x86");
        ARCH_MAPPING.put(Arch.ARM32, "arm");
        ARCH_MAPPING.put(Arch.AARCH64, "aarch64");
    }

    public String getDownloadUrl(OperatingSystem os, Arch arch) {
        return String.format(
            "https://api.adoptium.net/v3/binary/version/%s/%s/%s/jre/hotspot/normal/eclipse?project=jdk",
            this.versionString,
            OS_MAPPING.get(os),
            ARCH_MAPPING.get(arch)
        );
    }

    @Override
    public String toString() {
        return StringUtil.prettifyHeader(this.name().toLowerCase()); // Hey, it works!
    }

}
