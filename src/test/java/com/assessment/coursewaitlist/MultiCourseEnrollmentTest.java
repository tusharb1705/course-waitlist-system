package com.assessment.coursewaitlist;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MultiCourseEnrollmentTest {
    @Test
    void enrollmentMapAndWaitlistsAreIndependentPerCourse() {
        Course java = new Course("Java", 1);
        Course math = new Course("Math", 2);
        var service = new EnrollmentService(List.of(java, math));
        for (String id : List.of("A", "B", "C")) service.updateStudent(new Student(id, true));
        assertEquals(EnrollmentResult.ENROLLED, service.enroll("Java", "A"));
        assertEquals(EnrollmentResult.WAITLISTED, service.enroll("Java", "B"));
        assertEquals(EnrollmentResult.ENROLLED, service.enroll("Math", "A"));
        assertEquals(EnrollmentResult.ENROLLED, service.enroll("Math", "C"));
        assertEquals(Map.of(java, List.of("A"), math, List.of("A", "C")), service.enrollment());
        assertEquals(List.of("B"), service.state("Java").waitlisted());
        assertTrue(service.state("Math").waitlisted().isEmpty());
        assertEquals("B", service.drop("Java", "A").promotedLearnerId().orElseThrow());
        assertEquals(List.of("B"), service.state("Java").enrolled());
        assertEquals(List.of("A", "C"), service.state("Math").enrolled());
    }

    @Test
    void membershipAndEligibilityAreScopedCorrectly() {
        var service = new EnrollmentService(List.of(new Course("Java", 1), new Course("Math", 1)));
        service.updateStudent(new Student("A", true));
        service.updateStudent(new Student("B", true));
        service.enroll("Java", "A");
        service.enroll("Java", "B");
        assertEquals(EnrollmentResult.ALREADY_WAITLISTED, service.enroll("Java", "B"));
        assertEquals(EnrollmentResult.ENROLLED, service.enroll("Math", "B"));
        service.updateStudent(new Student("B", false));
        assertTrue(service.drop("Java", "A").promotedLearnerId().isEmpty());
        assertEquals(List.of("B"), service.state("Math").enrolled());
        assertTrue(service.state("Java").waitlisted().isEmpty());
    }

    @Test
    void rejectsDuplicateAndUnknownCoursesAndProtectsEnrollmentView() {
        assertThrows(IllegalArgumentException.class, () -> new EnrollmentService(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new EnrollmentService(List.of(
                new Course("Java", 1), new Course("Java", 2))));
        var service = new EnrollmentService(List.of(new Course("Java", 1)));
        assertThrows(IllegalArgumentException.class, () -> service.enroll("missing", "A"));
        assertThrows(IllegalArgumentException.class, () -> service.drop("missing", "A"));
        assertThrows(IllegalArgumentException.class, () -> service.state("missing"));
        assertThrows(UnsupportedOperationException.class, () -> service.enrollment().clear());
        assertThrows(UnsupportedOperationException.class, () -> service.enrollment().values().iterator().next().add("A"));
    }
}
