package info.kgeorgiy.ja.matveev.walk;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JenkinsHash extends HashCalculator {

    private final static int BUFFER_SIZE = 8192;
    private final byte[] buffer = new byte[BUFFER_SIZE];

    public JenkinsHash() {
        this.zeroHash = "0".repeat(8);
    }

    public String calculateHash(Path path) throws IOException {
        int hash = 0;
        if (Files.exists(path)) {
            BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(path));
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; ++i) {
                    hash += buffer[i] & 0xff;
                    hash += hash << 10;
                    hash ^= hash >>> 6;
                }
            }
            hash += hash << 3;
            hash ^= hash >>> 11;
            hash += hash << 15;
        }
        return String.format("%08x", hash);
    }
}
