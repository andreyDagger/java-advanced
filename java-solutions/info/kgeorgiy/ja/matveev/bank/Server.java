package info.kgeorgiy.ja.matveev.bank;

import info.kgeorgiy.ja.matveev.bank.bank.Bank;
import info.kgeorgiy.ja.matveev.bank.bank.RemoteBank;

import java.rmi.*;
import java.rmi.server.*;
import java.net.*;

/**
 * Server which hosts rmi bank.
 *
 * @author Andrey Matveev
 * @since 21
 */
public final class Server {
    private final static int DEFAULT_PORT = 8888;

    /**
     * Main function that runs program.
     * args[0] - port on which bank will be created.
     *
     * @param args Arguments in format described above
     */
    public static void main(final String... args) {
        final int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        final Bank bank = new RemoteBank(port);
        try {
            UnicastRemoteObject.exportObject(bank, port);
            Naming.rebind("//localhost/bank", bank);
            System.out.println("Server started");
        } catch (final RemoteException e) {
            System.out.println("Cannot export this object: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (final MalformedURLException e) {
            System.out.println("Malformed URL");
        }
    }
}
