package info.kgeorgiy.ja.matveev.bank.person;

import info.kgeorgiy.ja.matveev.bank.accont.RemoteAccount;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentMap;

/**
 * RemotePerson interface, uses rmi.
 *
 * @author Andrey Matveev
 * @since 21
 */
public interface RemotePerson extends Remote {
    /**
     * Getter for name
     *
     * @return person's name
     * @throws RemoteException if rmi broke
     */
    String getName() throws RemoteException;

    /**
     * Getter for surname
     *
     * @return person's surname
     * @throws RemoteException if rmi broke
     */
    String getSurname() throws RemoteException;

    /**
     * Getter for passportID
     *
     * @return person's passportID
     * @throws RemoteException if rmi broke
     */
    String getPassportID() throws RemoteException;

    /**
     * Getter for info
     *
     * @return person's info
     * @throws RemoteException if rmi broke
     */
    PersonInfo getInfo() throws RemoteException;

    /**
     * Getter for accounts
     *
     * @return person's accounts
     * @throws RemoteException if rmi broke
     */
    ConcurrentMap<String, RemoteAccount> getAccounts() throws RemoteException;

    /**
     * Creates a new account with given id.
     * If account already exists, return existing account with this id.
     *
     * @param accountId id of created account
     * @return created account
     * @throws RemoteException if rmi broke
     */
    RemoteAccount createAccount(final String accountId) throws RemoteException;

    /**
     * Returns account of this person with such id.
     *
     * @param accountID id of account
     * @return account with given id
     * @throws RemoteException if rmi broke
     */
    RemoteAccount getAccountByID(final String accountID) throws RemoteException;
}
