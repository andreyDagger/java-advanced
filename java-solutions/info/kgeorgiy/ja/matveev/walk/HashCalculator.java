package info.kgeorgiy.ja.matveev.walk;

import java.io.IOException;
import java.nio.file.Path;

abstract public class HashCalculator {
    String zeroHash;

    abstract String calculateHash(Path path) throws IOException;
}
