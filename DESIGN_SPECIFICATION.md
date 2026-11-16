# Design specification

## Scope and ownership

One `EnrollmentService` instance owns a nonempty list of immutable `Course` records, an enrollment map keyed by course, a waitlist map keyed by course, and shared student eligibility records. The caller should share that instance across threads. `Main` reads JSON configuration and handles output; the service performs no console or external I/O.

`Course` and `Student` are records. Course capacity must be positive. Names and learner identifiers must be nonblank. Student identifiers are case-sensitive and use `String.strip()` to remove surrounding whitespace. No additional identifier syntax is assumed.

## Prerequisite assumption

Each registered student has one Boolean prerequisite-completed status. `updateStudent(Student)` registers or replaces that status in an in-memory map keyed by identifier. An absent record is treated as ineligible. Only eligible learners can newly enroll, join the waitlist or be promoted.

The registry is the current source of truth: promotion consults it at the time of promotion, rather than storing status in the queue. Eligibility changes alone do not trigger promotion, reorder the queue, remove waiters or revoke existing enrollment. An enrolled learner who later becomes ineligible remains enrolled until dropping; the requirement governs admission and promotion, not automatic expulsion.

## State and invariants

- `Map<Course, Set<String>>` stores each course's unique enrolled identifiers in stable display order.
- `Map<Course, ArrayDeque<String>>` stores each course's waiting identifiers in FIFO order.
- `HashMap<String, Student>` stores the latest supplied prerequisite state.
- Each course's enrollment size is always between zero and its capacity.
- Each course's enrollment and waitlist contain no duplicates and are disjoint. The same learner may enroll in multiple courses.
- A nonempty waitlist for a course implies its seats are occupied after every public operation.

All mutable collections are private and never exposed. `state()` copies both membership collections under one lock. The returned lists are immutable and remain stable after subsequent changes. Course configuration and operation results are also immutable.

## Enrollment

`enroll(String courseName, String learnerId)` validates and normalizes the identifier, then checks enrollment and waitlist membership before checking eligibility. Existing members receive `ALREADY_ENROLLED` or `ALREADY_WAITLISTED`, even if their eligibility changed. Duplicate requests never update prerequisite status or queue order. New ineligible or unknown learners receive `PREREQUISITE_NOT_COMPLETED`. An eligible new learner receives `ENROLLED` if a seat is free, otherwise `WAITLISTED`.

This ordering gives repeat requests a stable explanation of current membership. Eligibility updates are explicit and separate from enrollment requests.

## Drop and FIFO promotion

`drop(String courseName, String learnerId)` returns `INVALID_LEARNER_ID` for null or blank identifiers and `NOT_ENROLLED` if the learner is absent from enrollment. Neither outcome changes the queue or promotes anyone. A waitlisted learner cannot cancel their waitlist entry through this operation.

A successful drop removes exactly one enrolled learner. If the queue is nonempty, remove its head and check current eligibility. Remove and report every ineligible head encountered, continuing until the first eligible learner is promoted or the queue is exhausted. Exactly one eligible learner can be promoted because one successful drop frees one seat. The result reports `DROPPED`, an optional promoted identifier and the skipped identifiers in FIFO order.

A skipped learner loses their queue position. If they become eligible later, they must request enrollment again and will join at the tail if full. A repeated drop cannot remove additional learners or cause another promotion.

## Synchronization

`enroll`, `drop`, `updateStudent`, `state` and `enrollment` synchronize on the same service instance. Validation, membership changes and promotion complete within one critical section. No thread can observe or use the temporary vacancy during a drop before its promotion is processed. Two concurrent requests for the final seat produce one enrollment and one waitlist entry.

Eligibility updates serialize with promotions: whichever operation acquires the lock first determines the status seen by that promotion. Immutable course access requires no lock. Private eligibility lookup is called only while the monitor is held.

The lock protects all courses in one service instance in one JVM. Separate instances do not coordinate even if configured with the same course name. There is no persistence, interprocess coordination, crash recovery or guarantee that concurrently arriving requests acquire the lock in wall-clock order. FIFO applies to the order in which waitlist entries are actually appended.

## Complexity and deliberate limits

Enrollment lookup in the set/map is expected O(1); checking a duplicate waiter is O(w) for w waiters. Appending is O(1). A drop scans at most the waiting list once, stopping after the first eligible learner. A snapshot costs O(e + w). A second waitlist membership set would improve lookup but add another invariant to maintain; a linear scan is appropriate for this small offering.

Student records remain in the registry after dropping. The registry size is bounded only by supplied identifiers. There is no waitlist cancellation, prerequisite engine, external integration, frontend or server.

## JSON input

The command-line demo reads `course-demo.json` from bundled resources by default or a user-supplied JSON path. The file defines a course list, shared student eligibility, and ordered enrollment and drop requests, each naming a course. `Main` validates the JSON types and rejects duplicate student definitions; business rules remain in `EnrollmentService`. An external file is read afresh on each run. A changed bundled resource is copied by Gradle before `run` and requires a rebuild for an installed distribution.

## Build decisions

The existing wrapper remains at Gradle 9.6.0. The application plugin includes Java support; the Java toolchain explicitly selects version 25. Group is `com.assessment`; package is `com.assessment.coursewaitlist`. JUnit BOM 5.13.4 replaces the initial 6.0.0 BOM to meet the JUnit 5 requirement. Test execution uses JUnit Platform and its launcher.

References: [Gradle Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html), [JUnit 5.13.4 guide](https://docs.junit.org/5.13.4/user-guide/).
