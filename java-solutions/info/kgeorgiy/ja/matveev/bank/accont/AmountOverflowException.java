package info.kgeorgiy.ja.matveev.bank.accont;

/**
 * Exception, which is thrown when money on account is greater than {@code Integer.MAX_VALUE}
 *
 * @author Andrey Matveev
 * @since 21
 */
public class AmountOverflowException extends Exception {
    /**
     * Constructor from String {@code message}.
     *
     * @param message Message that will be displayed
     */
    public AmountOverflowException(final String message) {
        super(message);
    }
}
