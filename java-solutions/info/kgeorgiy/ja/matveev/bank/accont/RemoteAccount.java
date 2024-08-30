package info.kgeorgiy.ja.matveev.bank.accont;

import java.rmi.*;

/**
 * Account interface
 *
 * @author Andrey Matveev
 * @since 21
 */
public interface RemoteAccount extends Remote {
    /** Returns account identifier. */
    String getId() throws RemoteException;

    /** Returns amount of money in the account. */
    long getAmount() throws RemoteException;

    /** Sets amount of money in the account. */
    void setAmount(final long amount) throws RemoteException, NegativeAmountException;

    /** Adds {@code amountDelta} money */
    void addAmount(final long amountDelta) throws RemoteException, NegativeAmountException, AmountOverflowException;
}
