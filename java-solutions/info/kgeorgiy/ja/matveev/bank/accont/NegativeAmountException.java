package info.kgeorgiy.ja.matveev.bank.accont;

/**
 * Exception, which is thrown when there is negative money on account.
 *
 * @author Andrey Matveev
 * @since 21
 */
public class NegativeAmountException extends Exception {
    /**
     * Constructor from String {@code message}.
     *
     * @param message Message that will be displayed
     */
    public NegativeAmountException(final String message) {
        super(message);
    }
}
