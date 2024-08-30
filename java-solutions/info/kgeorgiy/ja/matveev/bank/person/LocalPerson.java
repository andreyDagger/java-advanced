package info.kgeorgiy.ja.matveev.bank.person;

import info.kgeorgiy.ja.matveev.bank.accont.RemoteAccount;
import info.kgeorgiy.ja.matveev.bank.accont.Account;

import java.rmi.RemoteException;

/**
 * {@link } Person, that uses serialization
 *
 * @author Andrey Matveev
 * @since 21
 */
public class LocalPerson extends Person {

    /**
     * Constructor for LocalPerson from RemotePerson
     *
     * @param person remote person to copy
     */
    public LocalPerson(final RemotePerson person) throws RemoteException {
        super(person.getInfo());
        try {
            person.getAccounts().forEach((k, v) -> {
                try {
                    getAccounts().put(k, new Account(v));
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            throw (RemoteException) e.getCause();
        }
    }

    @Override
    public RemoteAccount createAccount(final String id) {
        final String accountID = getPassportID() + ":" + id;
        System.out.println("Creating remote account " + accountID);
        final Account account = new Account(accountID);
        if (getAccounts().putIfAbsent(accountID, account) == null) {
            return account;
        } else {
            return getAccountByID(id);
        }
    }
}