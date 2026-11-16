package com.assessment.coursewaitlist;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Main {
    private Main() { }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            throw new IllegalArgumentException("Usage: course-waitlist-system [config.json]");
        }
        try (InputStream input = args.length == 0
                ? Main.class.getResourceAsStream("/course-demo.json")
                : Files.newInputStream(Path.of(args[0]))) {
            if (input == null) {
                throw new IOException("Bundled course-demo.json was not found");
            }
            run(new ObjectMapper().readTree(input));
        }
    }

    private static void run(JsonNode config) {
        if (config == null || !config.isObject()) {
            throw new IllegalArgumentException("Configuration must be a JSON object");
        }
        List<Course> courses = new ArrayList<>();
        for (JsonNode entry : arrayField(config, "courses")) {
            if (!entry.isObject()) throw new IllegalArgumentException("Each course must be an object");
            courses.add(new Course(textField(entry, "name"), intField(entry, "capacity")));
        }
        EnrollmentService service = new EnrollmentService(courses);
        JsonNode students = arrayField(config, "students");
        Set<String> ids = new HashSet<>();
        for (JsonNode entry : students) {
            if (!entry.isObject()) throw new IllegalArgumentException("Each student must be an object");
            Student student = new Student(textField(entry, "id"), booleanField(entry, "prerequisiteCompleted"));
            if (!ids.add(student.id())) {
                throw new IllegalArgumentException("Duplicate student identifier: " + student.id());
            }
            service.updateStudent(student);
        }
        JsonNode enrollments = arrayField(config, "enrollmentRequests");
        JsonNode drops = arrayField(config, "dropRequests");
        service.courses().forEach(System.out::println);
        for (JsonNode entry : enrollments) {
            if (!entry.isObject()) throw new IllegalArgumentException("Each enrollment request must be an object");
            String course = textField(entry, "course");
            String id = textField(entry, "student");
            System.out.println("Enroll " + id + " in " + course + ": " + service.enroll(course, id));
        }
        for (JsonNode entry : drops) {
            if (!entry.isObject()) throw new IllegalArgumentException("Each drop request must be an object");
            String course = textField(entry, "course");
            String id = textField(entry, "student");
            System.out.println("Drop " + id + " from " + course + ": " + service.drop(course, id));
        }
        System.out.println("Final enrollment by course: " + service.enrollment());
        for (Course course : service.courses()) {
            System.out.println("Final waitlist for " + course.name() + ": " + service.state(course.name()).waitlisted());
        }
    }

    private static JsonNode objectField(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || !value.isObject()) throw new IllegalArgumentException(name + " must be an object");
        return value;
    }

    private static JsonNode arrayField(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || !value.isArray()) throw new IllegalArgumentException(name + " must be an array");
        return value;
    }

    private static String textField(JsonNode node, String name) {
        return textValue(node.get(name), name);
    }

    private static String textValue(JsonNode value, String name) {
        if (value == null || !value.isTextual()) throw new IllegalArgumentException(name + " must be a string");
        return value.textValue();
    }

    private static int intField(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) {
            throw new IllegalArgumentException(name + " must be an integer");
        }
        return value.intValue();
    }

    private static boolean booleanField(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || !value.isBoolean()) throw new IllegalArgumentException(name + " must be true or false");
        return value.booleanValue();
    }
}
