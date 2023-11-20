package dev.lukebemish.opensesame.compile.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Processor {
    public static void main(String[] args) {
        if ((~args.length & 1) != 1) {
            System.err.println("Usage: java dev.lukebemish.opensesame.compile.asm.Processor <input> <output> <input> <output> ...");
            System.exit(1);
        }
        for (int i = 0; i < args.length; i += 2) {
            var input = Path.of(args[0]);
            var output = Path.of(args[1]);
            if (Files.isRegularFile(input)) {
                try {
                    processFile(input, output);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try (var paths = Files.walk(input)) {
                    paths.filter(Files::isRegularFile).forEach(file -> {
                        try {
                            var relative = input.relativize(file);
                            var out = output.resolve(relative);
                            Files.createDirectories(out.getParent());
                            processFile(file, out);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void processFile(Path file, Path out) throws IOException {
        try (var inputStream = Files.newInputStream(file)) {
            ClassReader reader = new ClassReader(inputStream);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            reader.accept(new VisitingOpenProcessor(writer, VisitingOpenProcessor.ANNOTATIONS), 0);
            Files.write(out, writer.toByteArray());
        }
    }
}
