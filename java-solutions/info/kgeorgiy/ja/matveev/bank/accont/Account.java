package info.kgeorgiy.ja.matveev.bank.accont;

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * Serializible implementation of {@link RemoteAccount}
 *
 * @author Andrey Matveev
 * @since 21
 */
public class Account implements RemoteAccount, Serializable {
    private final String id;
    private long amount;

    /**
     * Copy constructor
     *
     * @param other Account which we are copying
     * @throws RemoteException if rmi broke
     */
    public Account(final RemoteAccount other) throws RemoteException {
        this.id = other.getId();
        this.amount = other.getAmount();
    }

    /**
     * Basic constructor from {@code id}, that creates account with zero money
     * @param id ID of account
     */
    public Account(final String id) {
        this.id = id;
        this.amount = 0;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getAmount() {
        System.out.println("Getting amount of money for account " + id);
        return amount;
    }

    @Override
    synchronized public void setAmount(final long amount) throws RemoteException, NegativeAmountException {
        if (amount < 0) {
            throw new NegativeAmountException("Negative amount on account: " + id);
        }
        System.out.println("Setting amount of money for account " + id);
        this.amount = amount;
    }

    @Override
    synchronized public void addAmount(final long amountDelta) throws RemoteException, NegativeAmountException, AmountOverflowException {
        try {
            final long newAmount = Math.addExact(getAmount(), amountDelta);
            setAmount(newAmount);
        } catch (final ArithmeticException e) {
            throw new AmountOverflowException(String.format("Can't set amount higher than %d on account %s", Long.MAX_VALUE, getId()));
        }
    }
}
