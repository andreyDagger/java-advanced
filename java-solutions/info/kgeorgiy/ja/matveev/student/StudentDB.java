package info.kgeorgiy.ja.matveev.student;

import info.kgeorgiy.java.advanced.student.*;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StudentDB implements StudentQuery, GroupQuery, AdvancedQuery {

    private static final Comparator<Student> NAMES_COMPARATOR = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparing(Comparator.comparing(Student::getId).reversed());

    private static final Comparator<Group> GROUP_SIZE_NAME_COMPARATOR = Comparator
            .comparing((Group group) -> group.getStudents().size())
            .thenComparing(Group::getName);

    private final Comparator<Group> GROUP_DISTINCT_FIRST_NAMES_COMPARATOR = Comparator
            .comparing((Group group) -> getDistinctFirstNames(group.getStudents()).size())
            .thenComparing(Comparator.comparing(Group::getName).reversed());

    private static final Comparator<Map.Entry<String, Set<GroupName>>> MOST_POPULAR_NAME_COMPARATOR = Comparator.comparing((Map.Entry<String, Set<GroupName>> e) -> e.getValue().size())
            .thenComparing(Map.Entry.<String, Set<GroupName>>comparingByKey().reversed());

    private static final Comparator<Map.Entry<String, Set<GroupName>>> LEAST_POPULAR_NAME_COMPARATOR = Comparator.comparing((Map.Entry<String, Set<GroupName>> e) -> e.getValue().size()).reversed()
            .thenComparing(Map.Entry.<String, Set<GroupName>>comparingByKey().reversed());

    @Override
    public List<String> getFirstNames(List<Student> list) { // NOTE: copypaste
        return list.stream()
                .map(Student::getFirstName)
                .toList();
    }

    @Override
    public List<String> getLastNames(List<Student> list) {
        return list.stream()
                .map(Student::getLastName)
                .toList();
    }

    @Override
    public List<GroupName> getGroups(List<Student> list) {
        return list.stream()
                .map(Student::getGroup)
                .toList();
    }

    @Override
    public List<String> getFullNames(List<Student> list) {
        return list.stream()
                .map(i -> i.getFirstName() + " " + i.getLastName())
                .toList();
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> list) {
        return list.stream()
                .map(Student::getFirstName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> list) {
        return list.stream()
                .max(Comparator.comparingInt(Student::getId))
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> collection) {
        return collection.stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> collection) {
        return collection.stream()
                .sorted(NAMES_COMPARATOR)
                .toList();
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> collection, String s) { // NOTE: copypaste
        return sortStudentsByName(collection.stream()
                .filter(i -> i.getFirstName().equals(s))
                .toList());
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> collection, String s) {
        return sortStudentsByName(collection.stream()
                .filter(i -> i.getLastName().equals(s))
                .toList());
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> collection, GroupName groupName) {
        return sortStudentsByName(collection.stream()
                .filter(i -> i.getGroup().equals(groupName))
                .toList());
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> collection, GroupName groupName) {
        return collection.stream()
                .filter(i -> i.getGroup().equals(groupName))
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(Comparator.naturalOrder())));
    }

    private List<Group> getGroupsByComparator(Collection<Student> collection, Comparator<Student> comparator) {
        return collection.stream()
                .collect(Collectors.groupingBy(Student::getGroup))
                .entrySet()
                .stream()
                .map(entry -> new Group(entry.getKey(), entry.getValue()
                        .stream()
                        .sorted(comparator)
                        .toList()))
                .sorted(Comparator.comparing(Group::getName))
                .toList();
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> collection) {
        return getGroupsByComparator(collection, NAMES_COMPARATOR);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> collection) {
        return getGroupsByComparator(collection, Comparator.naturalOrder());
    }

    @Override
    public GroupName getLargestGroup(Collection<Student> collection) {
        return getGroupsByName(collection).stream()
                .max(GROUP_SIZE_NAME_COMPARATOR)
                .map(Group::getName)
                .orElse(null);
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> collection) {
        return getGroupsByName(collection).stream()
                .max(GROUP_DISTINCT_FIRST_NAMES_COMPARATOR)
                .map(Group::getName)
                .orElse(null);
    }

    private String getPopularName(Collection<Student> collection, boolean most) {
        return collection.stream()
                .collect(() -> new HashMap<String, Set<GroupName>>(),
                        (o, i) -> o.computeIfAbsent(i.getFirstName(), k -> new HashSet<>()).add(i.getGroup()),
                        (a1, a2) -> a2.forEach((key, value) -> a1.computeIfAbsent(key, k -> new HashSet<>()).addAll(value)))
                .entrySet()
                .stream()
                .max(most ? MOST_POPULAR_NAME_COMPARATOR : LEAST_POPULAR_NAME_COMPARATOR)
                .map(Map.Entry::getKey)
                .orElse("");
    }

    @Override
    public String getMostPopularName(Collection<Student> collection) {
        return getPopularName(collection, true);
    }

    @Override
    public String getLeastPopularName(Collection<Student> collection) {
        return getPopularName(collection, false);
    }

    private <T> List<T> getSubsequence(Collection<Student> collection, int[] ints, Function<Student, T> mapper) {
        return IntStream.of(ints)
                .mapToObj(i -> collection.stream().skip(i).findFirst().get())
                .map(mapper)
                .toList();
    }

    @Override
    public List<String> getFirstNames(Collection<Student> collection, int[] ints) {
        return getSubsequence(collection, ints, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(Collection<Student> collection, int[] ints) {
        return getSubsequence(collection, ints, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(Collection<Student> collection, int[] ints) {
        return getSubsequence(collection, ints, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(Collection<Student> collection, int[] ints) {
        return getSubsequence(collection, ints, s -> String.format("%s %s", s.getFirstName(), s.getLastName()));
    }
}
