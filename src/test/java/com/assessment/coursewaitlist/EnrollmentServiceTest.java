package com.assessment.coursewaitlist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.assessment.coursewaitlist.EnrollmentResult.*;
import static org.junit.jupiter.api.Assertions.*;

class EnrollmentServiceTest {
    private EnrollmentService service(int capacity, String... eligibleIds) {
        EnrollmentService service = new EnrollmentService(new Course("Java", capacity));
        for (String id : eligibleIds) {
            service.updateStudent(new Student(id, true));
        }
        return service;
    }

    private void assertState(EnrollmentService service, List<String> enrolled, List<String> waiting) {
        EnrollmentService.State state = service.state();
        assertEquals(enrolled, state.enrolled());
        assertEquals(waiting, state.waitlisted());
        assertTrue(state.enrolled().size() <= service.course().capacity());
        assertEquals(state.enrolled().size(), new HashSet<>(state.enrolled()).size());
        assertEquals(state.waitlisted().size(), new HashSet<>(state.waitlisted()).size());
        assertTrue(state.enrolled().stream().noneMatch(state.waitlisted()::contains));
    }

    @Test
    void enrollsWhenSeatAvailableAndQueuesWhenFull() {
        var service = service(1, "A", "B");
        assertEquals(ENROLLED, service.enroll("A"));
        assertState(service, List.of("A"), List.of());
        assertEquals(WAITLISTED, service.enroll("B"));
        assertState(service, List.of("A"), List.of("B"));
    }

    @Test
    void duplicateRequestsDoNotChangeStateOrQueueOrder() {
        var service = service(1, "A", "B", "C");
        service.enroll("A");
        service.enroll("B");
        service.enroll("C");
        assertEquals(ALREADY_ENROLLED, service.enroll("A"));
        assertEquals(ALREADY_WAITLISTED, service.enroll("B"));
        assertState(service, List.of("A"), List.of("B", "C"));
    }

    @Test
    void promotesInFifoOrderAcrossMultipleDrops() {
        var service = service(2, "A", "B", "C", "D");
        for (String id : List.of("A", "B", "C", "D")) service.enroll(id);
        var first = service.drop("B");
        assertEquals(DropResult.Status.DROPPED, first.status());
        assertEquals("C", first.promotedLearnerId().orElseThrow());
        assertTrue(first.removedIneligibleLearnerIds().isEmpty());
        assertState(service, List.of("A", "C"), List.of("D"));
        assertEquals("D", service.drop("A").promotedLearnerId().orElseThrow());
        assertState(service, List.of("C", "D"), List.of());
    }

    @Test
    void rejectsIneligibleAndUnknownLearners() {
        var service = service(1);
        service.updateStudent(new Student("A", false));
        assertEquals(PREREQUISITE_NOT_COMPLETED, service.enroll("A"));
        assertEquals(PREREQUISITE_NOT_COMPLETED, service.enroll("unknown"));
        assertState(service, List.of(), List.of());
    }

    @Test
    void rejectsIneligibleLearnerEvenWhenFull() {
        var service = service(1, "A");
        service.enroll("A");
        service.updateStudent(new Student("B", false));
        assertEquals(PREREQUISITE_NOT_COMPLETED, service.enroll("B"));
        assertState(service, List.of("A"), List.of());
    }

    @Test
    void removesNewlyIneligibleFrontLearnersAndPromotesNextEligible() {
        var service = service(1, "A", "B", "C", "D");
        for (String id : List.of("A", "B", "C", "D")) service.enroll(id);
        service.updateStudent(new Student("B", false));
        service.updateStudent(new Student("C", false));
        assertState(service, List.of("A"), List.of("B", "C", "D"));
        var result = service.drop("A");
        assertEquals(List.of("B", "C"), result.removedIneligibleLearnerIds());
        assertEquals("D", result.promotedLearnerId().orElseThrow());
        assertState(service, List.of("D"), List.of());
    }

    @Test
    void usesLatestEligibilityWhenLearnerBecomesEligibleAgain() {
        var service = service(1, "A", "B");
        service.enroll("A");
        service.enroll("B");
        service.updateStudent(new Student("B", false));
        assertEquals(ALREADY_WAITLISTED, service.enroll("B"));
        service.updateStudent(new Student("B", true));
        assertEquals("B", service.drop("A").promotedLearnerId().orElseThrow());
        assertState(service, List.of("B"), List.of());
    }

    @Test
    void removesEntireIneligibleQueueAndLeavesSeatAvailable() {
        var service = service(1, "A", "B", "C");
        for (String id : List.of("A", "B", "C")) service.enroll(id);
        service.updateStudent(new Student("B", false));
        service.updateStudent(new Student("C", false));
        var result = service.drop("A");
        assertTrue(result.promotedLearnerId().isEmpty());
        assertEquals(List.of("B", "C"), result.removedIneligibleLearnerIds());
        assertState(service, List.of(), List.of());
        service.updateStudent(new Student("B", true));
        assertEquals(ENROLLED, service.enroll("B"));
        assertState(service, List.of("B"), List.of());
    }

    @Test
    void dropsWithEmptyQueueAndAllowsReenrollment() {
        var service = service(1, "A");
        service.enroll("A");
        var result = service.drop("A");
        assertEquals(DropResult.Status.DROPPED, result.status());
        assertTrue(result.promotedLearnerId().isEmpty());
        assertTrue(result.removedIneligibleLearnerIds().isEmpty());
        assertState(service, List.of(), List.of());
        assertEquals(ENROLLED, service.enroll("A"));
        assertState(service, List.of("A"), List.of());
    }

    @Test
    void nonEnrolledAndRepeatedDropsCannotRemoveWaitersOrPromote() {
        var service = service(1, "A", "B", "C");
        for (String id : List.of("A", "B", "C")) service.enroll(id);
        for (String id : List.of("B", "unknown")) {
            var result = service.drop(id);
            assertEquals(DropResult.Status.NOT_ENROLLED, result.status());
            assertTrue(result.promotedLearnerId().isEmpty());
            assertTrue(result.removedIneligibleLearnerIds().isEmpty());
            assertState(service, List.of("A"), List.of("B", "C"));
        }
        service.drop("A");
        assertEquals(DropResult.Status.NOT_ENROLLED, service.drop("A").status());
        assertState(service, List.of("B"), List.of("C"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "\u2003"})
    void invalidOperationIdentifiersLeaveStateUnchanged(String id) {
        var service = service(1, "A", "B");
        service.enroll("A");
        service.enroll("B");
        assertEquals(INVALID_LEARNER_ID, service.enroll(id));
        var result = service.drop(id);
        assertEquals(DropResult.Status.INVALID_LEARNER_ID, result.status());
        assertTrue(result.promotedLearnerId().isEmpty());
        assertTrue(result.removedIneligibleLearnerIds().isEmpty());
        assertState(service, List.of("A"), List.of("B"));
    }

    @Test
    void stripsSurroundingWhitespaceAndKeepsIdentifiersCaseSensitive() {
        var service = service(2, " A ", "a");
        assertEquals(ENROLLED, service.enroll(" A "));
        assertEquals(ALREADY_ENROLLED, service.enroll("A"));
        assertEquals(ENROLLED, service.enroll("a"));
        assertState(service, List.of("A", "a"), List.of());
        assertEquals(DropResult.Status.DROPPED, service.drop(" A ").status());
        assertState(service, List.of("a"), List.of());
    }

    @Test
    void rejectsInvalidConfigurationAndStudentRecords() {
        assertThrows(IllegalArgumentException.class, () -> new Course("Java", 0));
        assertThrows(IllegalArgumentException.class, () -> new Course("Java", -1));
        assertThrows(IllegalArgumentException.class, () -> new Course(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new Course(" ", 1));
        assertThrows(IllegalArgumentException.class, () -> new Student(null, true));
        assertThrows(IllegalArgumentException.class, () -> new Student(" ", true));
        assertThrows(NullPointerException.class, () -> new EnrollmentService((Course) null));
        assertThrows(NullPointerException.class, () -> service(1).updateStudent(null));
    }

    @Test
    void snapshotsAreImmutableAndDoNotChangeAfterLaterOperations() {
        var service = service(1, "A", "B");
        service.enroll("A");
        service.enroll("B");
        var before = service.state();
        assertThrows(UnsupportedOperationException.class, () -> before.enrolled().clear());
        assertThrows(UnsupportedOperationException.class, () -> before.waitlisted().clear());
        var drop = service.drop("A");
        assertThrows(UnsupportedOperationException.class, () -> drop.removedIneligibleLearnerIds().add("X"));
        assertEquals(List.of("A"), before.enrolled());
        assertEquals(List.of("B"), before.waitlisted());
        assertState(service, List.of("B"), List.of());
    }

    @Test
    void concurrentRequestsForFinalSeatNeverExceedCapacity() throws Exception {
        var service = service(2, "existing", "A", "B");
        service.enroll("existing");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var requests = List.of("A", "B").stream().map(id -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("Start latch timed out");
                var result = service.enroll(id);
                var state = service.state();
                assertTrue(state.enrolled().size() <= 2);
                assertTrue(state.enrolled().stream().noneMatch(state.waitlisted()::contains));
                return result;
            })).toList();
            try {
                assertTrue(ready.await(5, TimeUnit.SECONDS), "Workers did not become ready");
            } finally {
                start.countDown();
            }
            var results = List.of(requests.get(0).get(5, TimeUnit.SECONDS),
                    requests.get(1).get(5, TimeUnit.SECONDS));
            assertEquals(1, results.stream().filter(r -> r == ENROLLED).count());
            assertEquals(1, results.stream().filter(r -> r == WAITLISTED).count());
            var state = service.state();
            assertEquals("existing", state.enrolled().getFirst());
            assertEquals(2, state.enrolled().size());
            assertEquals(1, state.waitlisted().size());
            assertEquals(new HashSet<>(List.of("existing", "A", "B")),
                    new HashSet<>(java.util.stream.Stream.concat(
                            state.enrolled().stream(), state.waitlisted().stream()).toList()));
            assertState(service, state.enrolled(), state.waitlisted());
        }
    }
}
