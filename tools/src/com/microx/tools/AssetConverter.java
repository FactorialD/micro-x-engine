package com.microx.tools;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/** Desktop-only converter. Editable inputs never enter the MIDlet resource tree. */
public final class AssetConverter {
    private AssetConverter() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: AssetConverter <source-dir> <output-dir>");
        final Path source = Paths.get(args[0]).toAbsolutePath().normalize();
        final Path output = Paths.get(args[1]).toAbsolutePath().normalize();
        Files.createDirectories(output);
        try (Stream<Path> paths = Files.walk(source)) {
            paths.filter(Files::isRegularFile).sorted().forEach(path -> convert(source, output, path));
        }
    }

    private static void convert(Path root, Path output, Path input) {
        try {
            Path relative = root.relativize(input);
            String name = relative.getFileName().toString();
            if (name.endsWith(".level")) {
                writeLevel(input, output.resolve(replaceSuffix(relative, ".level", ".lvl")));
            } else if (name.endsWith(".obj")) {
                writeModel(input, output.resolve(replaceSuffix(relative, ".obj", ".mesh")));
            }
            // Editor metadata and every unknown source format are intentionally ignored.
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path replaceSuffix(Path path, String oldSuffix, String newSuffix) {
        String value = path.toString();
        return Paths.get(value.substring(0, value.length() - oldSuffix.length()) + newSuffix);
    }

    private static void writeLevel(Path input, Path output) throws IOException {
        List<String> lines = Files.readAllLines(input, StandardCharsets.UTF_8);
        StringBuilder runtime = new StringBuilder();
        for (String line : lines) {
            int comment = line.indexOf('#');
            String clean = (comment < 0 ? line : line.substring(0, comment)).trim();
            if (!clean.isEmpty()) runtime.append(clean).append('\n');
        }
        if (!runtime.toString().startsWith("MXL1\n")) throw new IOException("Invalid MXL1 level: " + input);
        Files.createDirectories(output.getParent());
        Files.write(output, runtime.toString().getBytes(StandardCharsets.US_ASCII));
    }

    private static void writeModel(Path input, Path output) throws IOException {
        List<String> vertices = new ArrayList<String>();
        List<String> faces = new ArrayList<String>();
        for (String line : Files.readAllLines(input, StandardCharsets.US_ASCII)) {
            line = line.trim();
            if (line.startsWith("v ")) vertices.add(line.substring(2).trim());
            else if (line.startsWith("f ")) faces.add(line.substring(2).trim().replace('/', ' '));
        }
        Files.createDirectories(output.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.US_ASCII)) {
            writer.write("MXM1 " + vertices.size() + " " + faces.size()); writer.newLine();
            for (String vertex : vertices) { writer.write("v " + vertex); writer.newLine(); }
            for (String face : faces) { writer.write("f " + face); writer.newLine(); }
        }
    }
}
