package info.kgeorgiy.ja.matveev.crawler;

import java.net.MalformedURLException;

public class UncheckedMalformedUrlException extends RuntimeException {
    public UncheckedMalformedUrlException(final MalformedURLException e) {
        super(e);
    }
}
