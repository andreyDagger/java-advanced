package info.kgeorgiy.ja.matveev.bank;

import info.kgeorgiy.ja.matveev.bank.accont.RemoteAccount;
import info.kgeorgiy.ja.matveev.bank.accont.AmountOverflowException;
import info.kgeorgiy.ja.matveev.bank.accont.NegativeAmountException;
import info.kgeorgiy.ja.matveev.bank.bank.Bank;
import info.kgeorgiy.ja.matveev.bank.person.PersonInfo;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Client class, that creates interacts with remote bank
 *
 * @author Andrey Matveev
 * @since 21
 */
public final class Client {
    /** Utility class. */
    private Client() {}

    /**
     * Main function that runs program.
     * args[0] - Name
     * args[1] - Surname
     * args[2] - passportID
     * args[3] - acountID
     *
     * @param args arguments in format given above
     * @throws RemoteException if rmi broke
     * @throws MalformedURLException if bank address is invalid
     * @throws NotBoundException if bank object wasn't bound to expected address
     */
    public static void main(final String... args) throws RemoteException, MalformedURLException, NotBoundException {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("args and his elements must be non-null");
            System.exit(1);
        }
        final Bank bank;
        try {
            bank = (Bank) Naming.lookup("//localhost/bank");
        } catch (final NotBoundException e) {
            System.out.println("Bank is not bound");
            throw e;
        } catch (final MalformedURLException e) {
            System.out.println("Bank URL is invalid");
            throw e;
        } catch (final RemoteException e) {
            System.err.println(e.getMessage());
            throw e;
        }

        final String name = args.length > 0 ? args[0] : "Andrey";
        final String surname = args.length > 1 ? args[1] : "Matveev";
        final String passportID = args.length > 2 ? args[2] : "228322";
        final String accountID = args.length > 3 ? args[3] : "1488";
        final int amountDelta;
        try {
            amountDelta = args.length > 4 ? Integer.parseInt(args[4]) : 42069;
        } catch (final NumberFormatException e) {
            System.err.println("amountDelta must be som integer value");
            return;
        }

        var person = bank.getRemotePersonByPassport(passportID);
        if (person == null) {
            System.out.println("Creating person");
            person = bank.createPerson(new PersonInfo(name, surname, passportID));
        } else {
            System.out.println("Account already exists");
            if (!name.equals(person.getName()) || !surname.equals(person.getSurname()) || !passportID.equals(person.getPassportID())) {
                System.err.println("Your person data doesn't coincide with person data in bank");
                return;
            }
        }
        RemoteAccount account = person.getAccountByID(accountID);
        if (account == null) {
            System.out.println("Creating account");
            account = person.createAccount(accountID);
        } else {
            System.out.println("Account already exists");
        }
        System.out.println("Person: " + person);
        System.out.println("Account id: " + account.getId());
        System.out.println("Money before: " + account.getAmount());
        System.out.println("Adding money");
        try {
            account.addAmount(amountDelta);
        } catch (final NegativeAmountException | AmountOverflowException e) {
            System.err.println(e.getMessage());
            return;
        }
        System.out.println("Money after: " + account.getAmount());
    }
}
