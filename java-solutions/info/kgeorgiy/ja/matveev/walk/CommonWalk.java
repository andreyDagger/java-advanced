package info.kgeorgiy.ja.matveev.walk;

import java.io.*;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;

public class CommonWalk {

    public static void run(String[] args, boolean isRecursive) {
        HashCalculator hashCalculator = null;
        if (args == null || args.length < 2 || args.length > 3) {
            // System.err.println("Wrong number of arguments. Usage: input_file output_file [hash_function]");
            return;
        }
        for (int i = 0; i < args.length; ++i) {
            if (args[i] == null) {
                // System.err.printf("Argument %d is null%n", i);
                return;
            }
        }
        if (args.length == 2 || args[2].equals("jenkins")) {
            hashCalculator = new JenkinsHash();
        } else if (args[2].equals("sha-1")) {
            try {
                hashCalculator = new Sha1Hash();
            } catch (NoSuchAlgorithmException e) {
                // System.err.println("Sorry, we don't have sha-1 implementation");
                System.exit(1);
            }
        } else {
            // System.err.println("Unknown algorithm " + args[2]);
            return;
        }

        String inputFile = args[0];
        String outputFile = args[1];
        Path outputPath, inputPath;
        try {
            outputPath = Paths.get(outputFile);
        } catch (InvalidPathException e) {
            // System.err.printf("EXCEPTION: file %s has invalid path. %s", outputFile, e);
            return;
        }
        try {
            inputPath = Paths.get(inputFile);
        } catch (InvalidPathException e) {
            // System.err.println("EXCEPTION: " + e);
            return;
        }
        try {
            if (outputPath.getParent() != null) {
                Files.createDirectories(outputPath.getParent());
            }
            Files.createFile(outputPath);
        } catch (FileAlreadyExistsException e) {
            // System.err.println("WARNING: File " + outputFile + " already exists. Content will be overwritten");
        } catch (IOException | SecurityException ignored) {
        }

        try (HashWriter hashWriter = new HashWriter(outputPath)) {
            Visitor visitor = new Visitor(hashCalculator, hashWriter);

            try (var bufferedReader = Files.newBufferedReader(inputPath)) {
                String filename;
                while ((filename = bufferedReader.readLine()) != null) {
                    Path filePath;
                    try {
                        filePath = Paths.get(filename);
                    } catch (InvalidPathException e) {
                        hashWriter.writeHash(filename, hashCalculator.zeroHash);
                        continue;
                    }
                    try {
                        if (isRecursive) {
                            Files.walkFileTree(filePath, visitor);
                        } else {
                            if (Files.isDirectory(filePath) || !Files.exists(filePath)) {
                                hashWriter.writeHash(filename, hashCalculator.zeroHash);
                            } else {
                                Files.walkFileTree(filePath, visitor);
                            }
                        }
                    } catch (IOException | SecurityException e) {
                        // System.out.println("EXCEPTION: Couldn't write result to output file " + outputFile);
                    }
                }
            } catch (IOException | SecurityException e) {
                // System.out.println("EXCEPTION: Couldn't read line from input file " + inputFile);
            } finally {
                try {
                    hashWriter.close();
                } catch (IOException | SecurityException e) {
                    // System.err.println("EXCEPTION: Couldn't close output file: " + outputFile);
                }
            }
        } catch (IOException | SecurityException e) {
            // System.err.println("EXCEPTION: Couldn't open file " + outputFile + " for writing");
        }
    }
}
