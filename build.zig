const std = @import("std");

const TargetInfo = struct {
    cpu_arch: std.Target.Cpu.Arch,
    os_tag: std.Target.Os.Tag,
    key: []const u8,
};

fn t(cpu_arch: std.Target.Cpu.Arch, os_tag: std.Target.Os.Tag, key: []const u8) TargetInfo {
    const query = TargetInfo{
        .cpu_arch = cpu_arch,
        .os_tag = os_tag,
        .key = key,
    };
    return query;
}

pub fn build(b: *std.Build) void {
    const optimize = std.builtin.OptimizeMode.ReleaseSmall;

    const jniHeaders = b.addSystemCommand(&.{
        "bash",
        "retrieve_headers.sh",
    });

    const targets = [_]TargetInfo{
        t(.x86_64, .linux, "linux-x86_64"),
        t(.aarch64, .linux, "linux-aarch64"),
        t(.x86, .linux, "linux-i386"),
        t(.arm, .linux, "linux-arm"),
        t(.x86_64, .windows, "windows-x86_64"),
        t(.aarch64, .windows, "windows-aarch64"),
        t(.x86, .windows, "windows-i386"),
        t(.x86_64, .macos, "macos-x86_64"),
        t(.aarch64, .macos, "macos-aarch64"),
    };

    for (targets) |target| {
        const lib = b.addSharedLibrary(.{
            .name = "opensesamenative",
            .target = b.resolveTargetQuery(.{
                .cpu_arch = target.cpu_arch,
                .os_tag = target.os_tag,
            }),
            .optimize = optimize,
        });
        lib.step.dependOn(&jniHeaders.step);
        lib.linkLibC();
        lib.addCSourceFiles(.{
            .files = &.{"src/opensesamenative.c"},
            .flags = &.{
                "-ffunction-sections",
                "-fdata-sections",
                "-nostdlib",
                "-nobuiltininc",
            },
        });
        lib.link_gc_sections = true;
        lib.addIncludePath(b.path("build//jni_headers/src/java.base/share/native/include"));
        if (target.os_tag == std.Target.Os.Tag.linux or target.os_tag == std.Target.Os.Tag.macos) {
            lib.addIncludePath(b.path("build/jni_headers/src/java.base/unix/native/include"));
        } else if (target.os_tag == .windows) {
            lib.addIncludePath(b.path("build/jni_headers/src/java.base/windows/native/include"));
        }

        b.getInstallStep().dependOn(&b.addInstallArtifact(lib, .{
            .dest_dir = .{
                .override = .{
                    .custom = target.key,
                },
            },
            .implib_dir = .disabled,
            .h_dir = .disabled,
        }).step);
    }
}
