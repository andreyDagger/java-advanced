package info.kgeorgiy.ja.matveev.implementor;

import java.io.IOException;
import java.io.Writer;

/**
 * Writer, that converts all unicode symbols with code greater than 128 to format \\u"code".
 */
public class ToUnicodeWriter extends Writer implements AutoCloseable {
    /**
     * {@link Writer} that is used to write
     */
    private final Writer writer;

    /**
     * @param writer Writer that is used to write
     */
    ToUnicodeWriter(Writer writer) {
        this.writer = writer;
    }

    /**
     * Writes array of characters
     *
     * @param cbuf Array of characters
     * @param off  Offset from which to start writing characters
     * @param len  Number of characters to write
     * @throws IOException If we couldn't write
     */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        writer.write(cbuf, off, len);
    }

    /**
     * Convert String to our special format
     *
     * @param s string that we want to convert
     * @return Converted string
     */
    private static String toUnicode(String s) {
        StringBuilder res = new StringBuilder();
        for (char c : s.toCharArray()) {
            res.append(c >= 128 ? String.format("\\u%04X", (int) c) : c);
        }
        return res.toString();
    }

    /**
     * Writes string in our special format
     *
     * @param s String to be written
     * @throws IOException If we couldn't write
     */
    @Override
    public void write(String s) throws IOException {
        writer.write(toUnicode(s));
    }

    /**
     * Flushes writer
     *
     * @throws IOException If we couldn't flush
     */
    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    /**
     * Closes writer
     *
     * @throws IOException if we couldn't close
     */
    @Override
    public void close() throws IOException {
        writer.close();
    }
}
