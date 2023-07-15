package co.casterlabs.kaimen.projectbuilder;

import java.util.Map;

import co.casterlabs.commons.platform.ArchFamily;
import co.casterlabs.commons.platform.OSDistribution;
import co.casterlabs.rakurai.StringUtil;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum JavaVersion {
    JAVA11("jdk-11.0.13%2B8");

    private static final Map<OSDistribution, String> OS_MAPPING = Map.of(
        OSDistribution.WINDOWS_NT, "windows",
        OSDistribution.MACOS, "mac",
        OSDistribution.LINUX, "linux"
    );

    private static final Map<String, String> ARCH_MAPPING = Map.of(
        ArchFamily.X86.getArchTarget(64), "x64",
        ArchFamily.X86.getArchTarget(32), "x86",
        ArchFamily.ARM.getArchTarget(32), "arm",
        ArchFamily.ARM.getArchTarget(64), "aarch64"
    );

    private String versionString;

    public String getDownloadUrl(OSDistribution os, String target) {
        return String.format(
            "https://api.adoptium.net/v3/binary/version/%s/%s/%s/jre/hotspot/normal/eclipse?project=jdk",
            this.versionString,
            OS_MAPPING.get(os),
            ARCH_MAPPING.get(target)
        );
    }

    @Override
    public String toString() {
        return StringUtil.prettifyHeader(this.name().toLowerCase()); // Hey, it works!
    }

}
