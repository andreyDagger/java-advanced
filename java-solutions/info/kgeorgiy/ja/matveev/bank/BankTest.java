package info.kgeorgiy.ja.matveev.bank;

import info.kgeorgiy.ja.matveev.bank.accont.RemoteAccount;
import info.kgeorgiy.ja.matveev.bank.accont.AmountOverflowException;
import info.kgeorgiy.ja.matveev.bank.accont.NegativeAmountException;
import info.kgeorgiy.ja.matveev.bank.bank.Bank;
import info.kgeorgiy.ja.matveev.bank.bank.RemoteBank;
import info.kgeorgiy.ja.matveev.bank.person.PersonInfo;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BankTest {
    private final Random random = new Random('a' + 'n' + 'd' + 'r' + 'e' + 'y' + 'D' + 'a' + 'g' + 'g' + 'e' + 'r'); // https://codeforces.com/profile/andreyDagger добавляйте в друзья
    private Bank bank;

    private String randomString(int from, int to) {
        int length = random.nextInt(from, to);
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    List<PersonInfo> people = List.of(
            new PersonInfo("Andrey", "Matveev", "228322"),
            new PersonInfo("Georgiy", "Korneev", "42069"),
            new PersonInfo("Kirill", "Konovalov", "НЕ_ЗАХОТЕЛ_РАЗГЛАШАТЬ_ДАННЫЕ"),
            new PersonInfo("Vladislav", "Dmitriev", "المشاركين"),
            new PersonInfo("rm", " -r", "f"),
            new PersonInfo("ぁあぃ", "いづづ", "ぴふめケ"));

    @BeforeAll
    public static void installRegistry() throws RemoteException {
        LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
    }

    @BeforeEach
    public void setupBank() throws IOException, NotBoundException {
        ServerSocket serverSocket = new ServerSocket(0);
        final int BANK_PORT = serverSocket.getLocalPort();
        serverSocket.close();

        bank = new RemoteBank(BANK_PORT);
        UnicastRemoteObject.exportObject(bank, BANK_PORT);
        Naming.rebind("//localhost/bank", bank);
        bank = (Bank) Naming.lookup("//localhost/bank");
        System.out.println("Server started");
    }

    @Test
    public void testCreatePersons() throws RemoteException {
        for (final PersonInfo info : people) {
            var person = bank.createPerson(info);
            Assertions.assertEquals(person.getInfo(), info);
        }
    }

    @Test
    public void testAddPositiveMoney() throws RemoteException {
        for (final PersonInfo info : people) {
            var person = bank.createPerson(info);
            String accountID = randomString(10, 20);
            RemoteAccount account = person.createAccount(accountID);
            Assertions.assertEquals(account.getAmount(), 0);
            int sum = 0;
            for (int iters = 0; iters < 100; ++iters) {
                int delta = random.nextInt(0, Integer.MAX_VALUE / 100);
                sum += delta;
                try {
                    account.addAmount(delta);
                } catch (final NegativeAmountException | AmountOverflowException ignored) {
                    throw new AssertionError("This is impossible");
                }
                Assertions.assertEquals(account.getAmount(), sum);
            }
        }
    }

    @Test
    public void testNegativeMoney() throws RemoteException {
        for (final PersonInfo info : people) {
            var person = bank.createPerson(info);
            String accountID = randomString(10, 20);
            RemoteAccount account = person.createAccount(accountID);
            Assertions.assertEquals(account.getAmount(), 0);
            for (int iters = 0; iters < 100; ++iters) {
                int delta = random.nextInt(Integer.MIN_VALUE, 0);
                Assertions.assertThrows(NegativeAmountException.class, () -> account.addAmount(delta), "negative amount must throw exception");
                Assertions.assertEquals(account.getAmount(), 0);
            }
        }
    }

    @Test
    public void testPersonMultipleAccounts() throws RemoteException {
        for (final PersonInfo info : people) {
            var person = bank.createPerson(info);
            for (int iters = 0; iters < 100; ++iters) {
                String accountID = randomString(10, 20);
                RemoteAccount account = person.createAccount(accountID);
                Assertions.assertEquals(person.getAccountByID(accountID), account);
            }
        }
    }

    @Test
    public void testGetPersonByPassport() throws RemoteException {
        for (final PersonInfo info : people) {
            var person = bank.createPerson(info);
            Assertions.assertEquals(bank.getRemotePersonByPassport(info.passportID()), person);
        }
    }

    @Test
    public void testDuplicatePersonCreation() throws RemoteException {
        for (final PersonInfo info : people) {
            var person1 = bank.createPerson(info);
            var person2 = bank.createPerson(info);
            Assertions.assertEquals(person1, person2);
        }
    }

    @Test
    public void testDuplicateAccountCreation() throws RemoteException {
        for (final PersonInfo info : people) {
            var person = bank.createPerson(info);
            String accountID = randomString(10, 20);
            RemoteAccount account1 = person.createAccount(accountID);
            RemoteAccount account2 = person.createAccount(accountID);
            Assertions.assertEquals(account1, account2);
        }
    }

    @Test
    public void testPersonUnderDrugs() throws RemoteException {
        PersonInfo saddamHussein = new PersonInfo("チヂヒ", "ـشـحـج", "{какой-то паспорт}");
        var person = bank.createPerson(saddamHussein);
        List<RemoteAccount> accounts = new ArrayList<>();
        for (int i = 0; i < 100; ++i) {
            RemoteAccount account = person.createAccount(randomString(10, 20));
            accounts.add(account);
        }
        for (final RemoteAccount account : accounts) {
            long sum = 0;
            for (int i = 0; i < 10; ++i) {
                int delta = (account + Integer.toString(i)).hashCode();
                boolean overflow = false;
                try {
                    Math.addExact(sum, delta);
                } catch (ArithmeticException e) {
                    overflow = true;
                }
                if (sum + delta < 0) {
                    Assertions.assertThrows(NegativeAmountException.class, () -> account.addAmount(delta));
                } else if (overflow) {
                    Assertions.assertThrows(AmountOverflowException.class, () -> account.addAmount(delta));
                } else {
                    sum += delta;
                    try {
                        account.addAmount(delta);
                    } catch (final Exception e) {
                        Assertions.fail("Mustn't throw");
                    }
                    Assertions.assertEquals(sum, account.getAmount());
                }
            }
        }
    }

    @Test
    public void testApp() {
        ExecutorService pool = Executors.newFixedThreadPool(people.size());
        for (final PersonInfo info : people) {
            String accountID = randomString(10, 20);
            int delta = random.nextInt(0, 100);
            Runnable task = () -> {
                try {
                    Client.main(info.name(), info.surname(), info.passportID(), accountID, Integer.toString(delta));
                } catch (final Exception e) {
                    Assertions.fail();
                }
            };
            pool.submit(task);
        }
        final long EXPECTED_TIMEOUT = 3;
        try {
            pool.shutdown();
            boolean done = pool.awaitTermination(EXPECTED_TIMEOUT, TimeUnit.SECONDS);
            Assertions.assertTrue(done);
        } catch (InterruptedException e) {
            Assertions.fail("App timeout");
        }
    }

    @Test
    public void testLocalPerson() throws RemoteException {
        for (PersonInfo info : people) {
            var person = bank.createPerson(info);
            var remotePerson = bank.getRemotePersonByPassport(info.passportID());
            var localPerson = bank.getLocalPersonByPassport(info.passportID());
            Assertions.assertEquals(person.getName(), localPerson.getName());
            Assertions.assertEquals(person.getName(), remotePerson.getName());
        }
    }

    @Test
    public void testLocalPerson2() throws RemoteException {
        for (PersonInfo info : people) {
            bank.createPerson(info);
            var remotePerson = bank.getRemotePersonByPassport(info.passportID());
            var localPerson = bank.getLocalPersonByPassport(info.passportID());
            String accountID = randomString(10, 20);
            remotePerson.createAccount(accountID);
            RemoteAccount account = remotePerson.getAccountByID(accountID);
            RemoteAccount localAccount = localPerson.getAccountByID(accountID);
            Assertions.assertNull(localAccount);
            Assertions.assertEquals(account.getAmount(), 0);
        }
    }

    @Test
    public void testLocalPerson3() throws RemoteException {
        for (PersonInfo info : people) {
            var person = bank.createPerson(info);
            String accountID = randomString(10, 20);
            RemoteAccount account = person.createAccount(accountID);
            var localPerson = bank.getLocalPersonByPassport(info.passportID());
            try {
                account.addAmount(123);
            } catch (final Exception e) {
                Assertions.fail();
            }
            Assertions.assertEquals(localPerson.getAccountByID(accountID).getAmount(), 0);
            Assertions.assertEquals(person.getAccountByID(accountID).getAmount(), 123);
        }
    }

    @Test
    public void testTransfer() throws RemoteException, NegativeAmountException, AmountOverflowException {
        var person1 = bank.createPerson(people.get(0));
        var person2 = bank.createPerson(people.get(1));
        String accountID1 = randomString(10, 20);
        String accountID2 = randomString(10, 20);
        RemoteAccount acc1 = person1.createAccount(accountID1);
        RemoteAccount acc2 = person2.createAccount(accountID2);
        acc1.addAmount(123);
        acc2.addAmount(456);
        bank.transfer(acc2, acc1, 56);
        Assertions.assertEquals(acc1.getAmount(), 123 + 56);
        Assertions.assertEquals(acc2.getAmount(), 456 - 56);
    }

    @Test
    public void testTransferFail() throws RemoteException, NegativeAmountException, AmountOverflowException {
        var person1 = bank.createPerson(people.get(0));
        var person2 = bank.createPerson(people.get(1));
        String accountID1 = randomString(10, 20);
        String accountID2 = randomString(10, 20);
        RemoteAccount acc1 = person1.createAccount(accountID1);
        RemoteAccount acc2 = person2.createAccount(accountID2);
        acc1.addAmount(Long.MAX_VALUE - 500);
        acc2.addAmount(456);
        Assertions.assertThrows(NegativeAmountException.class, () -> bank.transfer(acc2, acc1, 500));
        acc2.addAmount(200);
        Assertions.assertThrows(AmountOverflowException.class, () -> bank.transfer(acc2, acc1, 550));
    }

    @Test
    public void testFuzzTransfer() throws RemoteException, NegativeAmountException, AmountOverflowException {
        var person1 = bank.createPerson(people.get(0));
        var person2 = bank.createPerson(people.get(1));
        String accountID1 = randomString(10, 20);
        String accountID2 = randomString(10, 20);
        RemoteAccount acc1 = person1.createAccount(accountID1);
        RemoteAccount acc2 = person2.createAccount(accountID2);
        long amount1 = random.nextInt(0, Integer.MAX_VALUE);
        long amount2 = random.nextInt(0, Integer.MAX_VALUE);
        acc1.addAmount((int)amount1);
        acc2.addAmount((int)amount2);
        for (int iters = 0; iters < 200; ++iters) {
            int delta = random.nextInt(0, Integer.MAX_VALUE);
            if (amount1 - delta < 0) {
                Assertions.assertThrows(NegativeAmountException.class, () -> bank.transfer(acc1, acc2, delta));
            } else if (amount2 + delta > Integer.MAX_VALUE) {
                Assertions.assertThrows(AmountOverflowException.class, () -> bank.transfer(acc1, acc2, delta));
            } else {
                amount1 -= delta;
                amount2 += delta;
                bank.transfer(acc1, acc2, delta);
            }
            Assertions.assertEquals(acc1.getAmount(), amount1);
            Assertions.assertEquals(acc2.getAmount(), amount2);
        }
    }

    private static class Action {
        private final RemoteAccount from, to;
        private final int amount;

        public Action(RemoteAccount from, RemoteAccount to, int amount) {
            this.from = from;
            this.to = to;
            this.amount = amount;
        }
    }
    @Test
    public void testFuzzTransferMultithreading() throws RemoteException, NegativeAmountException, AmountOverflowException, InterruptedException {
        List<RemoteAccount> accounts = new ArrayList<>();
        List<Long> expectedAmount = new ArrayList<>();
        final long INITIAL_MONEY = 100000000;
        final long PERFORMANCE_CONST = 5;
        final long THREADS = 10;

        for (final PersonInfo info : people) {
            var person = bank.createPerson(info);
            RemoteAccount account = person.createAccount(randomString(10, 20));
            accounts.add(account);
            account.addAmount(INITIAL_MONEY);
            expectedAmount.add(INITIAL_MONEY);
        }

        int n = 10000;
        Queue<Action> actions = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; ++i) {
            int i1 = random.nextInt(0, accounts.size());
            int i2 = random.nextInt(0, accounts.size());
            int amount = random.nextInt(0, 10);
            RemoteAccount from = accounts.get(i1);
            RemoteAccount to = accounts.get(i2);
            actions.add(new Action(from, to, amount));
            expectedAmount.set(i1, expectedAmount.get(i1) - amount);
            expectedAmount.set(i2, expectedAmount.get(i2) + amount);
        }

        ExecutorService pool = Executors.newFixedThreadPool(10);
        for (int i = 0; i < THREADS; ++i) {
            Runnable task = () -> {
                while (!actions.isEmpty()) {
                    Action action = actions.poll();
                    if (action == null) {
                        break;
                    }
                    try {
                        System.out.printf("ACTION: %s ; %s ; %d %n", action.from.getId(), action.to.getId(), action.amount);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        bank.transfer(action.from, action.to, action.amount);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            pool.submit(task);
        }

        pool.shutdown();
        if (!pool.awaitTermination(n * PERFORMANCE_CONST, TimeUnit.MILLISECONDS)) {
            Assertions.fail("too slow");
        }
        for (int i = 0; i < accounts.size(); ++i) {
            Assertions.assertEquals(accounts.get(i).getAmount(), expectedAmount.get(i));
        }
    }

    @Test
    public void testDeadlock() throws RemoteException, NegativeAmountException, AmountOverflowException, InterruptedException {
        var person1 = bank.createPerson(people.get(0));
        var person2 = bank.createPerson(people.get(1));
        RemoteAccount acc1 = person1.createAccount("A");
        RemoteAccount acc2 = person2.createAccount("B");
        acc1.addAmount(100000000);
        acc2.addAmount(100000000);
        final int ITERS = 10000;
        final int PERFORMANCE_CONST = 5;
        Runnable task1 = () -> {
            for (int iter = 0; iter < ITERS; ++iter) {
                try {
                    bank.transfer(acc1, acc2, acc1.getAmount() + random.nextInt(0, 2));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Runnable task2 = () -> {
            for (int iter = 0; iter < ITERS; ++iter) {
                try {
                    bank.transfer(acc2, acc1, acc2.getAmount() + random.nextInt(0, 2));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(task1);
        pool.submit(task2);
        pool.shutdown();
        if (!pool.awaitTermination(ITERS * PERFORMANCE_CONST, TimeUnit.MILLISECONDS)) {
            Assertions.fail("Too slow or deadlock");
        }
    }
}