package com.assessment.coursewaitlist;

/** Immutable configuration for one course offering. */
public record Course(String name, int capacity) {
    public Course {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Course name must not be blank");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Course capacity must be positive");
        }
        name = name.strip();
    }
}
