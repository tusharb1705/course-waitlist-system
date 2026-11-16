package com.assessment.coursewaitlist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    private String output(String... args) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream captured = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            try { Main.main(args); } finally { System.setOut(original); }
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void bundledDemoShowsIndependentCoursesAndPromotion() throws Exception {
        String text = output();
        assertTrue(text.contains("Course[name=Java Fundamentals, capacity=2]"));
        assertTrue(text.contains("Course[name=Algorithms, capacity=1]"));
        assertTrue(text.contains("Enroll Tushar in Algorithms: ENROLLED"));
        assertTrue(text.contains("Enroll Bhardwaj in Algorithms: WAITLISTED"));
        assertTrue(text.contains("Drop Tushar from Algorithms: DropResult[status=DROPPED, promotedLearnerId=Optional[Bhardwaj]"));
        assertTrue(text.contains("Final enrollment by course:"));
        assertTrue(text.contains("Final waitlist for Algorithms: []"));
    }

    @Test
    void readsCustomCoursesAndPrerequisites(@TempDir Path directory) throws Exception {
        Path config = directory.resolve("custom.json");
        Files.writeString(config, """
                {
                  "courses": [{ "name": "Custom", "capacity": 1 }, { "name": "Other", "capacity": 1 }],
                  "students": [
                    { "id": "One", "prerequisiteCompleted": true },
                    { "id": "Two", "prerequisiteCompleted": false },
                    { "id": "Three", "prerequisiteCompleted": true }
                  ],
                  "enrollmentRequests": [
                    { "course": "Custom", "student": "One" },
                    { "course": "Custom", "student": "Two" },
                    { "course": "Custom", "student": "Three" },
                    { "course": "Other", "student": "One" }
                  ],
                  "dropRequests": [{ "course": "Custom", "student": "One" }]
                }
                """);
        String text = output(config.toString());
        assertTrue(text.contains("Enroll Two in Custom: PREREQUISITE_NOT_COMPLETED"));
        assertTrue(text.contains("Drop One from Custom: DropResult[status=DROPPED, promotedLearnerId=Optional[Three]"));
        assertTrue(text.contains("Final waitlist for Custom: []"));
        assertTrue(text.contains("Final waitlist for Other: []"));
        assertTrue(text.contains("Course[name=Other, capacity=1]=[One]"));
    }

    @Test
    void rejectsMalformedPrerequisiteValues(@TempDir Path directory) throws Exception {
        Path config = directory.resolve("bad.json");
        Files.writeString(config, """
                {
                  "courses": [{ "name": "Custom", "capacity": 1 }],
                  "students": [{ "id": "One", "prerequisiteCompleted": "yes" }],
                  "enrollmentRequests": [],
                  "dropRequests": []
                }
                """);
        var error = assertThrows(IllegalArgumentException.class, () -> output(config.toString()));
        assertTrue(error.getMessage().contains("prerequisiteCompleted"));
    }
}
