package info.kgeorgiy.ja.matveev.bank.bank;

import info.kgeorgiy.ja.matveev.bank.accont.RemoteAccount;
import info.kgeorgiy.ja.matveev.bank.accont.AmountOverflowException;
import info.kgeorgiy.ja.matveev.bank.accont.NegativeAmountException;
import info.kgeorgiy.ja.matveev.bank.person.LocalPerson;
import info.kgeorgiy.ja.matveev.bank.person.PersonInfo;
import info.kgeorgiy.ja.matveev.bank.person.RemotePerson;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Bank interface, supports creating {@link info.kgeorgiy.ja.matveev.bank.person.Person} and money transfers between theirs {@link RemoteAccount}
 *
 * @author Andrey Matveev
 * @since 21
 */
public interface Bank extends Remote {

    /**
     * Creates person with given {@code info}
     *
     * @param info Information about person: {@link PersonInfo}
     * @return {@link RemotePerson}, this person will be remote: all changes after this method will be seen
     * @throws RemoteException if rmi broke
     */
    RemotePerson createPerson(final PersonInfo info) throws RemoteException;

    /**
     * Finds person by his passport.
     * Returns his remote version
     *
     * @param passportID Person's passport
     * @return {@link RemotePerson}, this person will be remote: all changes after this method will be seen
     * @throws RemoteException if rmi broke
     */
    RemotePerson getRemotePersonByPassport(final String passportID) throws RemoteException;

    /**
     * Finds person by his passport.
     * Returns his local version.
     *
     * @param passportID Person's passport
     * @return {@link LocalPerson}, this person will be local: all changes after this method will NOT be seen
     * @throws RemoteException if rmi broke
     */
    LocalPerson getLocalPersonByPassport(final String passportID) throws RemoteException;

    /**
     * Transfers {@code amount} money from account {@code from} to account {@code to}.
     * If {@code amount < 0}, then transfers {@code -amount} money from account {@code to} to account {@code from}.
     *
     * @param from {@link RemoteAccount} from which we want to transfer money
     * @param to {@link RemoteAccount} to which we want to transfer money
     * @param amount amount of money to transfer
     * @throws RemoteException if rmi broke
     * @throws NegativeAmountException if somebody's money not enough to make transfer
     * @throws AmountOverflowException if somebody's money become greater that {@code Integer.MAX_VALUE}
     */
    void transfer(final RemoteAccount from, final RemoteAccount to, final long amount) throws RemoteException, NegativeAmountException, AmountOverflowException;
}
