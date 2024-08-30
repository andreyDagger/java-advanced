package info.kgeorgiy.ja.matveev.walk;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class Visitor extends SimpleFileVisitor<Path> {
    private final HashCalculator hashCalculator;
    private final HashWriter hashWriter;

    public Visitor(HashCalculator hashCalculator, HashWriter hashWriter) {
        this.hashCalculator = hashCalculator;
        this.hashWriter = hashWriter;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        String hash = hashCalculator.zeroHash;

        try {
            hash = hashCalculator.calculateHash(file);
        } catch (IOException ignored) {
            // System.err.printf("Couldn't calculate hash for file %s, so assuming that hash is zero", file);
        }

        hashWriter.writeHash(file.toString(), hash);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException ex) throws IOException {
        hashWriter.writeHash(file.toString(), hashCalculator.zeroHash);
        return FileVisitResult.CONTINUE;
    }
}
