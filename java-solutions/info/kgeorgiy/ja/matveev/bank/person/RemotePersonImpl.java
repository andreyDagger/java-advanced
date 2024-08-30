package info.kgeorgiy.ja.matveev.bank.person;

import info.kgeorgiy.ja.matveev.bank.accont.RemoteAccount;
import info.kgeorgiy.ja.matveev.bank.accont.Account;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Implementation of {@link RemotePerson}. Uses rmi
 *
 * @author Andrey Matveev
 * @since 21
 */
public class RemotePersonImpl extends Person {
    private final int port;

    /**
     * Constructor for RemotePersonImpl from info and port
     *
     * @param personInfo info contains: name, surname, passportID
     * @param port port on which remote person is located
     */
    public RemotePersonImpl(final PersonInfo personInfo, final int port) {
        super(personInfo);
        this.port = port;
    }

    @Override
    public RemoteAccount createAccount(final String id) throws RemoteException {
        final String accountID = getPassportID() + ":" + id;
        System.out.println("Creating remote account " + accountID);
        final Account account = new Account(accountID);
        if (getAccounts().putIfAbsent(accountID, account) == null) {
            UnicastRemoteObject.exportObject(account, port);
            return account;
        } else {
            return getAccountByID(id);
        }
    }
}