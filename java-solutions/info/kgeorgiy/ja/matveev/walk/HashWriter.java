package info.kgeorgiy.ja.matveev.walk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class HashWriter implements AutoCloseable {

    private final BufferedWriter bufferedWriter;

    public HashWriter(Path outputPath) throws IOException {
        bufferedWriter = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8);
    }

    public void close() throws IOException {
        bufferedWriter.close();
    }

    public void writeHash(String filename, String hash) throws IOException {
        bufferedWriter.write(String.format("%s %s%n", hash, filename));
    }
}
