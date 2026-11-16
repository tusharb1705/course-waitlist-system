package com.assessment.coursewaitlist;

/** Current prerequisite status supplied to the service; identifiers are case-sensitive. */
public record Student(String id, boolean prerequisiteCompleted) {
    public Student {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Student identifier must not be blank");
        }
        id = id.strip();
    }
}
