package info.kgeorgiy.ja.matveev.bank.bank;

import info.kgeorgiy.ja.matveev.bank.accont.RemoteAccount;
import info.kgeorgiy.ja.matveev.bank.accont.AmountOverflowException;
import info.kgeorgiy.ja.matveev.bank.accont.NegativeAmountException;
import info.kgeorgiy.ja.matveev.bank.person.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * RMI implementation of {@link Bank}.
 *
 * @author Andrey Matveev
 * @since 21
 */
public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, RemotePersonImpl> remotePersonByPassport = new ConcurrentHashMap<>();

    /**
     * Constructor for bank
     * @param port port on which bank will be created
     */
    public RemoteBank(final int port) {
        this.port = port;
    }

    @Override
    public RemotePerson createPerson(final PersonInfo info) throws RemoteException {
        System.out.printf("Creating remote person: %s %s %s%n", info.name(), info.surname(), info.passportID());
        final RemotePersonImpl person = new RemotePersonImpl(info, port);
        if (remotePersonByPassport.putIfAbsent(info.passportID(), person) == null) {
            UnicastRemoteObject.exportObject(person, port);
            return person;
        } else {
            return getRemotePersonByPassport(info.passportID());
        }
    }

    @Override
    public RemotePersonImpl getRemotePersonByPassport(final String passportID) {
        return remotePersonByPassport.get(passportID);
    }

    @Override
    public LocalPerson getLocalPersonByPassport(final String passportID) throws RemoteException {
        var person = remotePersonByPassport.get(passportID);
        return person == null ? null : new LocalPerson(person);
    }

    // amount >= 0
    private void transferAlreadySync(final RemoteAccount from, final RemoteAccount to, final long amount) throws NegativeAmountException, AmountOverflowException, RemoteException {
        from.addAmount(-amount);
        try {
            to.addAmount(amount);
        } catch (final AmountOverflowException e) {
            from.addAmount(amount);
            throw e;
        }
    }

    @Override
    public void transfer(final RemoteAccount from, final RemoteAccount to, final long amount) throws RemoteException, NegativeAmountException, AmountOverflowException {
        if (amount < 0) {
            transfer(to, from, -amount);
            return;
        }
        int comp = from.getId().compareTo(to.getId());
        if (comp == 0 || amount == 0) {
            return;
        }
        if (comp < 0) {
            synchronized (from) {
                synchronized (to) {
                    transferAlreadySync(from, to, amount);
                }
            }
        } else {
            synchronized (to) {
                synchronized (from) {
                    transferAlreadySync(from, to, amount);
                }
            }
        }
    }
}
