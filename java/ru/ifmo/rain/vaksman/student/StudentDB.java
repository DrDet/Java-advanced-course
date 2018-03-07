package ru.ifmo.rain.vaksman.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentGroupQuery {

    private List<String> getPropertiesList(List<Student> students, Function<Student, String> getter) {
        return students.stream()
                .map(getter)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getPropertiesList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getPropertiesList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return getPropertiesList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getPropertiesList(students, s -> s.getFirstName() + " " + s.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return getFirstNames(students).stream()
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream()
                .min(Comparator.naturalOrder())
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return students.stream()
                .sorted()
                .collect(Collectors.toList());
    }

    private List<Student> filteringSortByName(Collection<Student> students, Predicate<Student> predicate) {
        return students.stream()
                .sorted(
                    Comparator.comparing(Student::getLastName)
                        .thenComparing(Student::getFirstName)
                        .thenComparing(Comparator.naturalOrder())
                )
                .filter(predicate)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return filteringSortByName(students,
                s -> true);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filteringSortByName(students,
                s -> s.getFirstName().equals(name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filteringSortByName(students,
                s -> s.getLastName().equals(name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return filteringSortByName(students,
                s -> s.getGroup().equals(group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return students.stream()
                .filter(s -> s.getGroup().equals(group))
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(Comparator.naturalOrder()))
                );
    }

    private Stream<Map.Entry<String, List<Student>>> getEntryStream(Collection<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::getGroup)) // String -> List<Student>
                .entrySet()
                .stream();
    }

    private List<Group> getSortedGroupList(Collection<Student> students, Function<List<Student>, List<Student>> sorter) {
        return getEntryStream(students)
                .map(e -> new Group(e.getKey(), sorter.apply(e.getValue())))
                .sorted(Comparator.comparing(Group::getName))
                .collect(Collectors.toList());
    }

    private String getMaxGroup(Collection<Student> students, Function<List<Student>, Integer> counter) {
        return getEntryStream(students)
                .max(Comparator
                        .comparingInt((Map.Entry<String, List<Student>> e) -> counter.apply(e.getValue()))
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElse("");
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getSortedGroupList(students,
                this::sortStudentsByName
        );
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getSortedGroupList(students,
                this::sortStudentsById
        );
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getMaxGroup(students, List::size);
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getMaxGroup(students, s -> getDistinctFirstNames(s).size());
    }
}
