package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

@ApiStatus.Internal
class LookupProviderNative implements LookupProvider {
    static volatile boolean SETUP = false;

    private enum Architecture {
        I386("i386"),
        X86_64("x86_64"),
        ARM("arm"),
        AAARCH64("aarch64");

        private final String name;

        Architecture(String name) {
            this.name = name;
        }
    }

    private enum OperatingSystem {
        WINDOWS(Set.of(Architecture.I386, Architecture.X86_64, Architecture.AAARCH64)),
        LINUX(Set.of(Architecture.I386, Architecture.X86_64, Architecture.AAARCH64, Architecture.ARM)),
        MACOS(Set.of(Architecture.X86_64, Architecture.AAARCH64));

        private final Set<Architecture> architectures;

        OperatingSystem(Set<Architecture> architectures) {
            this.architectures = architectures;
        }
    }

    private static synchronized void setup() throws IOException {
        if (SETUP) {
            return;
        }
        String osName = System.getProperty("os.name").toLowerCase();
        OperatingSystem os;
        if (osName.startsWith("windows")) {
            os = OperatingSystem.WINDOWS;
        } else if (osName.startsWith("linux")) {
            os = OperatingSystem.LINUX;
        } else if (osName.startsWith("mac")) {
            os = OperatingSystem.MACOS;
        } else {
            throw new IllegalStateException("Unsupported operating system for opensesame native lookup provider: " + osName);
        }

        Architecture arch;
        String osArch = System.getProperty("os.arch");
        boolean is64Bit = osArch.contains("64") || osArch.startsWith("armv8");
        if (osArch.startsWith("aarch") || osArch.startsWith("arm")) {
            arch = is64Bit ? Architecture.AAARCH64 : Architecture.ARM;
        } else if (osArch.startsWith("ppc") || osArch.startsWith("riscv")) {
            throw new IllegalStateException("Unsupported architecture for opensesame native lookup provider: " + osArch);
        } else {
            arch = is64Bit ? Architecture.X86_64 : Architecture.I386;
        }

        if (!os.architectures.contains(arch)) {
            throw new IllegalStateException("Unsupported architecture and operating system combination for opensesame native lookup provider: " + osArch + ", " + osName);
        }

        String fileName = switch (os) {
            case LINUX -> "libopensesamenative.so";
            case MACOS -> "libopensesamenative.dylib";
            case WINDOWS -> "opensesamenative.dll";
        };
        Path path = Files.createTempDirectory(null).resolve(fileName);
        String nativePath = "/dev/lukebemish/opensesame/runtime/"+os.name().toLowerCase(Locale.ROOT)+"/"+arch.name+"/"+fileName;
        var resource = LookupProviderNative.class.getResource(
                nativePath
        );
        if (resource == null) {
            throw new IOException("Could not find native library in opensesame jar, at path "+nativePath);
        }
        try (var lib = resource.openStream()) {
            Files.copy(lib, path);
        }
        System.load(path.toAbsolutePath().toString());
        SETUP = true;
    }

    private final MethodHandles.Lookup lookup;

    LookupProviderNative() {
        try {
            setup();
            this.lookup = NativeImplementations.nativeImplLookup();
        } catch (Throwable t) {
            throw new RuntimeException("Error calling native library", t);
        }
    }

    @Override
    public MethodHandles.Lookup openingLookup(MethodHandles.Lookup original, Class<?> target) {
        return this.lookup;
    }
}
