package com.assessment.coursewaitlist;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** One instance owns multiple offerings. All mutable state is guarded by this instance's monitor. */
public final class EnrollmentService {
    private final List<Course> courses;
    private final Map<String, Course> coursesByName = new HashMap<>();
    private final Map<String, Student> students = new HashMap<>();
    private final Map<Course, Set<String>> enrollment = new LinkedHashMap<>();
    private final Map<Course, ArrayDeque<String>> waitlists = new LinkedHashMap<>();

    public EnrollmentService(Course course) {
        this(List.of(Objects.requireNonNull(course, "course")));
    }

    public EnrollmentService(List<Course> courses) {
        this.courses = List.copyOf(courses);
        if (this.courses.isEmpty()) throw new IllegalArgumentException("At least one course is required");
        for (Course course : this.courses) {
            if (coursesByName.putIfAbsent(course.name(), course) != null) {
                throw new IllegalArgumentException("Duplicate course name: " + course.name());
            }
            enrollment.put(course, new LinkedHashSet<>());
            waitlists.put(course, new ArrayDeque<>());
        }
    }

    /** Registers or replaces current eligibility without changing enrollment or queue order. */
    public synchronized void updateStudent(Student student) {
        Objects.requireNonNull(student, "student");
        students.put(student.id(), student);
    }

    public List<Course> courses() {
        return courses;
    }

    public synchronized Map<Course, List<String>> enrollment() {
        Map<Course, List<String>> result = new LinkedHashMap<>();
        enrollment.forEach((course, ids) -> result.put(course, List.copyOf(ids)));
        return java.util.Collections.unmodifiableMap(result);
    }

    /** Unknown learners have no completed prerequisite and cannot join. */
    public synchronized EnrollmentResult enroll(String courseName, String learnerId) {
        Course course = requireCourse(courseName);
        Set<String> enrolled = enrollment.get(course);
        ArrayDeque<String> waitlist = waitlists.get(course);
        if (learnerId == null || learnerId.isBlank()) {
            return EnrollmentResult.INVALID_LEARNER_ID;
        }
        String id = learnerId.strip();
        if (enrolled.contains(id)) {
            return EnrollmentResult.ALREADY_ENROLLED;
        }
        if (waitlist.contains(id)) {
            return EnrollmentResult.ALREADY_WAITLISTED;
        }
        if (!isEligible(id)) {
            return EnrollmentResult.PREREQUISITE_NOT_COMPLETED;
        }
        if (enrolled.size() < course.capacity()) {
            enrolled.add(id);
            return EnrollmentResult.ENROLLED;
        }
        waitlist.addLast(id);
        return EnrollmentResult.WAITLISTED;
    }

    public synchronized DropResult drop(String courseName, String learnerId) {
        Course course = requireCourse(courseName);
        Set<String> enrolled = enrollment.get(course);
        ArrayDeque<String> waitlist = waitlists.get(course);
        if (learnerId == null || learnerId.isBlank()) {
            return new DropResult(DropResult.Status.INVALID_LEARNER_ID, Optional.empty(), List.of());
        }
        if (!enrolled.remove(learnerId.strip())) {
            return new DropResult(DropResult.Status.NOT_ENROLLED, Optional.empty(), List.of());
        }
        List<String> skipped = new ArrayList<>();
        while (enrolled.size() < course.capacity() && !waitlist.isEmpty()) {
            String candidate = waitlist.removeFirst();
            // Consult the current registry, never an eligibility snapshot taken at queue entry.
            if (!isEligible(candidate)) {
                skipped.add(candidate);
                continue;
            }
            enrolled.add(candidate);
            return new DropResult(DropResult.Status.DROPPED, Optional.of(candidate), skipped);
        }
        return new DropResult(DropResult.Status.DROPPED, Optional.empty(), skipped);
    }

    private boolean isEligible(String id) {
        Student student = students.get(id);
        return student != null && student.prerequisiteCompleted();
    }

    private Course requireCourse(String name) {
        Course course = name == null ? null : coursesByName.get(name.strip());
        if (course == null) throw new IllegalArgumentException("Unknown course: " + name);
        return course;
    }

    /** Compatibility helpers for a single-course instance. */
    public Course course() { return singleCourse(); }
    public EnrollmentResult enroll(String learnerId) { return enroll(singleCourse().name(), learnerId); }
    public DropResult drop(String learnerId) { return drop(singleCourse().name(), learnerId); }
    public State state() { return state(singleCourse().name()); }

    private Course singleCourse() {
        if (courses.size() != 1) throw new IllegalStateException("Specify a course name");
        return courses.getFirst();
    }

    /** Both collections are copied under the same lock for a consistent, immutable view. */
    public synchronized State state(String courseName) {
        Course course = requireCourse(courseName);
        return new State(List.copyOf(enrollment.get(course)), List.copyOf(waitlists.get(course)));
    }

    public record State(List<String> enrolled, List<String> waitlisted) {
        public State {
            enrolled = List.copyOf(enrolled);
            waitlisted = List.copyOf(waitlisted);
        }
    }
}
