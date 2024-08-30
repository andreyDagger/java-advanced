package info.kgeorgiy.ja.matveev.bank.person;

import info.kgeorgiy.ja.matveev.bank.accont.RemoteAccount;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class Person implements Serializable, RemotePerson  {
    private final PersonInfo info;
    private final ConcurrentMap<String, RemoteAccount> accountByID = new ConcurrentHashMap<>();

    public Person(final PersonInfo info) {
        this.info = info;
    }

    @Override
    public String getName() {
        return info.name();
    }

    @Override
    public String getSurname() {
        return info.surname();
    }

    @Override
    public String getPassportID() {
        return info.passportID();
    }

    @Override
    public PersonInfo getInfo() {
        return info;
    }

    @Override
    public ConcurrentMap<String, RemoteAccount> getAccounts() {
        return accountByID;
    }

    @Override
    public RemoteAccount getAccountByID(final String accountID) {
        return accountByID.get(getPassportID() + ":" + accountID);
    }

    @Override
    public abstract RemoteAccount createAccount(final String accountId) throws RemoteException;
}
