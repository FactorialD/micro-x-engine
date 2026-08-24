package com.microx.tools;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

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
            } else if (name.equals("textures.png")) {
                writeTexture(input, output.resolve(replaceSuffix(relative, ".png", ".tex")));
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
        List<float[]> vertices = new ArrayList<float[]>();
        List<int[]> faces = new ArrayList<int[]>();
        for (String line : Files.readAllLines(input, StandardCharsets.US_ASCII)) {
            line = line.trim();
            if (line.startsWith("v ")) {String[] p=line.substring(2).trim().split("\\s+");vertices.add(new float[]{Float.parseFloat(p[0]),Float.parseFloat(p[1]),Float.parseFloat(p[2])});}
            else if (line.startsWith("f ")) {String[] p=line.substring(2).trim().split("\\s+");if(p.length>=3)faces.add(new int[]{objIndex(p[0]),objIndex(p[1]),objIndex(p[2])});}
        }
        Files.createDirectories(output.getParent());
        try (DataOutputStream out=new DataOutputStream(Files.newOutputStream(output))) {
            out.writeInt(0x4d584d32);out.writeShort(1);out.writeShort(0);out.writeShort(0);out.writeShort(vertices.size());out.writeShort(faces.size());
            for(float[] v:vertices){out.writeInt((int)(v[0]*65536.0f));out.writeInt((int)(v[1]*65536.0f));out.writeInt((int)(v[2]*65536.0f));}
            for(int i=0;i<vertices.size();i++){out.writeInt(0);out.writeInt(0);}
            for(int[] face:faces)for(int index:face)out.writeShort(index);
        }
    }

    private static int objIndex(String token){int slash=token.indexOf('/');return Integer.parseInt(slash<0?token:token.substring(0,slash))-1;}

    private static void writeTexture(Path input,Path output)throws IOException{
        BufferedImage image=ImageIO.read(input.toFile());if(image==null||image.getWidth()>256||image.getHeight()>256)throw new IOException("Invalid texture atlas: "+input);
        Files.createDirectories(output.getParent());try(DataOutputStream out=new DataOutputStream(Files.newOutputStream(output))){out.writeInt(0x4d585432);out.writeShort(1);out.writeShort(image.getWidth());out.writeShort(image.getHeight());for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)out.writeInt(image.getRGB(x,y)&0xffffff);}
    }
}
