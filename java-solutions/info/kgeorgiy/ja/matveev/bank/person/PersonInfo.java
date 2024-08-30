package info.kgeorgiy.ja.matveev.bank.person;

import java.io.Serializable;

public record PersonInfo(String name, String surname, String passportID) implements Serializable {
}
