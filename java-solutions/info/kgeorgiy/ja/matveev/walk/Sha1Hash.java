package info.kgeorgiy.ja.matveev.walk;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha1Hash extends HashCalculator {
    private static final int BUFFER_SIZE = 8192;
    private static final byte[] buffer = new byte[BUFFER_SIZE];
    private final MessageDigest sha1;

    public Sha1Hash() throws NoSuchAlgorithmException {
        sha1 = MessageDigest.getInstance("SHA-1");
        zeroHash = "0".repeat(40);
    }

    public String calculateHash(Path path) throws IOException {
        sha1.reset();
        try (FileInputStream fis = new FileInputStream(path.toString())) {
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                sha1.update(buffer, 0, bytesRead);
            }
        }

        byte[] hash = sha1.digest();

        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
