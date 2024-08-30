module info.kgeorgiy.ja.matveev {
    requires java.compiler;
    requires java.rmi;
    requires jdk.httpserver;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.engine;
    requires org.junit.platform.commons;
    requires org.junit.platform.engine;
    requires org.junit.platform.launcher;

    requires info.kgeorgiy.java.advanced.student;
    requires info.kgeorgiy.java.advanced.implementor;
    requires info.kgeorgiy.java.advanced.iterative;
    requires info.kgeorgiy.java.advanced.mapper;
    requires info.kgeorgiy.java.advanced.crawler;
    requires info.kgeorgiy.java.advanced.hello;

    exports info.kgeorgiy.ja.matveev.bank to org.junit.platform.commons, java.rmi;
    exports info.kgeorgiy.ja.matveev.bank.person to java.rmi, org.junit.platform.commons;
    exports info.kgeorgiy.ja.matveev.bank.accont to java.rmi, org.junit.platform.commons;
    exports info.kgeorgiy.ja.matveev.bank.bank to java.rmi, org.junit.platform.commons;
}