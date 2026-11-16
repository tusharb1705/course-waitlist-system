# AI-ASSISTED DEVELOPMENT LOG

**Project Name:** Course Enrollment and FIFO Waitlist Management System  
**Candidate:** Tushar Bhardwaj  
**Assessment:** Focused AI-Assisted Engineering Assessment – Course Waitlist  
**Date:** 14 November 2026  
**Programming Language:** Java 25  
**Build Tool:** Gradle 9.6.0 (Kotlin DSL)  
**Testing Framework:** JUnit 5  
**IDE:** IntelliJ IDEA Ultimate  
**AI Tools:** OpenAI Codex and ChatGPT  
**Storage:** In-memory Java Collections

---

## 1. Project Overview

The objective of this project was to implement a reliable course enrollment and waitlist management service.

The application supports:

- Enrollment of eligible students when seats are available.
- Prerequisite eligibility validation.
- Prevention of duplicate enrollments.
- FIFO (First In, First Out) waitlist management.
- Dropping enrolled students.
- Automatic promotion of eligible waitlisted students when seats become available.
- Handling invalid requests and repeated operations.
- Thread-safe execution of enrollment operations.
- Automated unit testing.

The application was developed as a command-line prototype with a separate service layer.

Although the current implementation is a CLI application, the service was designed with future controller integration and concurrent requests in mind.

A database, frontend, and REST API were intentionally excluded to maintain the bounded scope of the assessment.

## 2. AI-Assisted Development Approach

I used OpenAI Codex inside IntelliJ IDEA Ultimate as an engineering assistant.

Instead of providing a single large prompt to generate the entire application, I followed an incremental development approach.

I gave Codex individual instructions for specific development activities, including:

1. Reviewing the existing project.
2. Creating the enrollment service.
3. Separating responsibilities into different classes.
4. Generating unit tests and targeting complete coverage.
5. Updating unit tests after changes to the implementation.

During development, I also reviewed the application behavior, identified an issue in the drop workflow, and manually improved the waiting queue implementation.

I independently raised the requirement for thread safety and considered how the service might behave if multiple requests reached it through a controller in the future.

AI assistance was used for implementation and testing support, while engineering decisions and the responsibility for verifying the final application remained with me.

## 3. Chronological AI Prompt and Development Record

### AI-001: Initial Project Review and CLI Setup

**Actual prompt given to Codex:**

> Read the existing code and lets start to create a simple CLI applications and Unit test cases I have already added the dependencies if something is old or irrelevant please remove

**Objective**

The purpose of this prompt was to have Codex understand the existing project before beginning the implementation.

I wanted to maintain a minimal application and avoid adding unnecessary frameworks or libraries.

**Requested AI activities**

- Review the existing project structure.
- Inspect the Gradle configuration.
- Work with the dependencies already present.
- Identify unnecessary or outdated dependencies.
- Prepare a simple CLI application.
- Set up automated unit testing.

**Observed implementation**

The project contains:

- Gradle Kotlin DSL configuration.
- A Java command-line entry point.
- JUnit-based automated tests.
- A separate service layer.

**Engineering decision**

I chose to keep the implementation lightweight and focus on the features required by the assessment rather than introduce a full application framework.

**Verification**

The final project includes `Main.java`, Gradle build configuration, and JUnit tests.

The exact dependencies changed by Codex require confirmation from the original conversation or version history.

**AI suggestion status:** Implementation present.

---

### AI-002: Student Enrollment Service

**Actual prompt given to Codex:**

> now lets start with StudentEnrollment create a service which handles the list of students , List of Courses and waiting queue and a map of enrolled students with course and list of students in that course

**Objective**

The goal was to introduce a separate service responsible for managing enrollment and waiting-list functionality.

I wanted the service to manage student details, course information, enrolled students, and waitlisted students.

**Requested AI activities**

- Create an enrollment service.
- Maintain registered students.
- Maintain course information.
- Track student enrollment.
- Manage the waiting queue.
- Keep business logic separate from the CLI application.

**Observed implementation**

The final uploaded project contains an `EnrollmentService` that manages:

- Student records.
- Multiple course offerings.
- Enrolled learner identifiers.
- A FIFO waiting queue.
- Enrollment and drop operations.

**Engineering decision**

Keeping enrollment operations inside a dedicated service separates business rules from the command-line interface.

This also makes the code easier to test and potentially reuse through a controller later.

---

### AI-003: Separation of Classes and Code Refactoring

**Actual prompt given to Codex:**

> divide the code in separate classes of Students , cources , DropResult , EnrollmentResult

**Objective**

I wanted to improve the structure and maintainability of the code by separating responsibilities into individual Java classes.

**Classes created in the resulting project**

**Student.java**

Represents student information, including the unique identifier and prerequisite eligibility.

**Course.java**

Represents the course offering and its capacity.

**EnrollmentResult.java**

Represents the result of an enrollment request.

**DropResult.java**

Provides structured information about a course-drop operation and any associated waitlist promotion.

**EnrollmentService.java**

Contains the core business logic for registration, waitlisting, dropping, and promotion.

**Engineering reasoning**

This separation supports:

- Object-oriented design.
- Clear responsibilities.
- Encapsulation.
- Readable and maintainable code.
- Easier unit testing.
- Future extension without tightly coupling business logic to the CLI.

**Verification**

The project contains these separate Java classes.

---

### AI-004: Unit Test Generation and Coverage

**Prompt described to Codex:**

> create test cases and achieve 100% line and branch coverage

**Objective**

My intention was to ensure that the application was tested thoroughly rather than merely verifying the normal enrollment flow.

I specifically targeted:

- 100% line coverage.
- 100% branch coverage.
- Positive and negative test scenarios.
- Error handling.
- Boundary conditions.
- Validation of enrollment and waitlist rules.

**Areas requiring test coverage**

- Enrollment when seats are available.
- Enrollment when capacity is full.
- Duplicate enrollment.
- Duplicate waitlist requests.
- FIFO promotion.
- Prerequisite failure.
- Invalid student identifiers.
- Dropping an enrolled student.
- Repeated drop requests.
- Promotion with ineligible waiting students.
- Concurrent enrollment requests.

**Observed implementation**

The project contains automated JUnit tests covering the main workflows and relevant edge cases.

The saved test reports show:

| Test suite | Tests | Failures |
|---|---:|---:|
| EnrollmentServiceTest | 19 | 0 |
| MainTest | 3 | 0 |
| MultiCourseEnrollmentTest | 3 | 0 |
| Total | 25 | 0 |

**Important coverage distinction**

My target was 100% line coverage.

However, passing all 25 tests does not independently establish 100% coverage.

**Measured line coverage:** 94%.

---

### DEV-005: Manual Debugging and Waitlist Refactoring

**Activity type:** Independent developer intervention.

**This was not an AI prompt.**

**Problem identified**

After executing the unit tests, I observed that the course-drop functionality was failing.

At that stage, the waiting-list implementation used an `ArrayList`.

I reviewed the drop logic and decided to simplify the handling of waiting students by using a queue-oriented data structure.

**Manual changes**

I modified the drop logic myself and introduced the following queue-based representation during refactoring:

```java
Map<Course, ArrayDeque<String>> waitingQueues;
```

The intention was to associate a course with an ordered queue of waiting student identifiers.

**Why I chose ArrayDeque**

The waiting-list requirement follows FIFO behavior.

`ArrayDeque` supports the operations required for this workflow:

```java
waitingQueue.addLast(studentId);
```

This adds a student to the end of the queue.

```java
String nextStudent = waitingQueue.pollFirst();
```

This removes and returns the first waiting student.

The queue structure makes FIFO processing more explicit and avoids needing to remove the first element from an `ArrayList`.

**My engineering contribution**

- Identified a failed drop workflow during testing.
- Reviewed the waiting-list implementation.
- Selected an appropriate FIFO collection.
- Manually revised the drop logic.
- Improved clarity of the queue operations.

**AI contribution**

The manual modification was developer-initiated. It should not be credited to Codex.

**Decision status:** Developer-initiated and manually edited.

---

### AI-006: Fixing Unit Tests After Refactoring

**Prompt described to Codex:**

> fix the unit test cases

**Background**

After manually correcting the waiting-list and drop logic, I instructed Codex to fix the unit tests.

The goal was to ensure that the tests correctly verified the revised implementation.

**Requested AI activities**

- Review the existing unit tests.
- Identify tests affected by the changes.
- Correct test cases where necessary.
- Preserve the intended business rules.
- Ensure successful execution of the test suite.

**Reported result**

After this development activity, all unit tests were running successfully.

The project results support this outcome with 25 recorded tests and no failures.

**Developer responsibility**

The tests must validate the assessment requirements rather than simply be modified to produce passing results.

In particular, FIFO promotion, capacity restrictions, prerequisite validation, and invalid drop behavior must remain correct.

**Verification**

The saved JUnit XML reports contain:

- 25 tests.
- 0 failures.
- 0 errors.
- 0 skipped tests.

The exact test changes made by Codex still require confirmation from its conversation history or a source-code diff.

**AI suggestion status:** Test suite present and passing in evidence.

---

## 4. Independent Engineering Decision: Thread Safety

One important design consideration that I raised was thread safety.

Although this assessment uses a CLI application, I considered the possibility that the same enrollment service could later be called through a controller.

In such a scenario, multiple requests may reach the service concurrently.

**Problem considered**

Suppose a course has only one seat available.

Two students attempt to enroll at approximately the same time.

Without proper synchronization, both operations could potentially observe the available seat before either updates the enrolled-student collection.

This could result in capacity being exceeded.

**My engineering decision**

I requested that the enrollment functionality be implemented with thread safety and concurrent-session handling in mind.

**Observed implementation**

The `EnrollmentService` uses synchronized operations to protect mutable enrollment and waitlist state.

The implementation synchronizes operations including:

- Enrollment.
- Dropping a student.
- Updating student information.
- Reading service state.

This protects the shared state of an individual service instance.

**Concurrency testing**

The project contains a test named:

`concurrentRequestsForFinalSeatNeverExceedCapacity`

The test uses `CountDownLatch` to coordinate concurrent requests.

It verifies that two competing enrollment requests do not cause the course to exceed its available capacity.

**Limitation**

The synchronization protects a shared service instance within one JVM.

It does not provide distributed synchronization between multiple application instances.

That would require additional design considerations if the service were expanded into a production system.

**Ownership**

The requirement to consider thread safety, concurrent sessions, and future controller integration originated from me.

---

## 5. Architecture and Technical Decisions

### 5.1 In-memory storage

The assessment permits in-memory storage when no starter project is provided.

I used Java Collections instead of introducing a database.

This kept the implementation simple and focused on the required behavior.

**Tradeoff:** Application state is lost when the program terminates.

### 5.2 FIFO queue management

The final implementation uses `ArrayDeque` to manage the waiting list.

This provides clear first-in, first-out ordering.

### 5.3 Duplicate enrollment prevention

The enrollment implementation uses a set-based representation to maintain unique enrolled learner identifiers.

The service also checks existing enrollment and waitlist membership before admitting a new request.

### 5.4 Prerequisite validation

The application uses a Boolean prerequisite-completion status.

Only eligible students may enroll.

When a seat opens, the waitlist promotion logic checks the student's current eligibility.

### 5.5 Ineligible waitlist policy

When promotion reaches an ineligible waiting learner, the implementation removes that learner from the queue and continues to the next eligible learner.

This policy is documented and applied consistently.

### 5.6 Service separation

The CLI entry point is separate from the business logic.

This allows the enrollment service to be tested without depending on console input or output.

### 5.7 Synchronization

The implementation uses synchronized service operations to protect mutable state from simultaneous access within one service instance.

---

## 6. Automated Testing and Verification

### 6.1 Testing framework

JUnit Jupiter (JUnit 5) is used for automated unit testing.

### 6.2 Test scenarios

The implementation includes tests covering:

1. Enrollment with available capacity.
2. Waitlisting when course capacity is full.
3. Duplicate enrollment requests.
4. Duplicate waitlist requests.
5. FIFO promotion across multiple drops.
6. Prerequisite validation failures.
7. Unknown students.
8. Ineligible students at the front of the waitlist.
9. Changes in prerequisite eligibility.
10. Empty waitlist behavior.
11. Invalid and repeated drop requests.
12. Invalid student identifiers.
13. Invalid configuration.
14. Immutable state snapshots.
15. Concurrent enrollment requests.
16. Expected CLI demonstration behavior.

### 6.3 test execution results

| Metric | Result |
|---|---|
| Total recorded tests | 25 |
| Passed | 25 |
| Failed | 0 |
| Errors | 0 |
| Skipped | 0 |

**Source of evidence:** JUnit XML reports found in the project.

These results represent an existing test execution record, not a fresh test run performed while writing this log.

### 6.4 Independent verification commands

The following commands can be used from the project root:

```bash
./gradlew clean test
```

```bash
./gradlew run
```

```bash
./gradlew build
```

The generated HTML test report can be reviewed at:

`build/reports/tests/test/index.html`

**Final personal verification record**

- Date and time: 14/11/2026 5:40 PM
- Gradle build: PASS
- JUnit test execution: PASS
- CLI execution: PASS
- Coverage report: 94% line coverage (as measured by Gradle's JaCoCo plugin)
- Manual review completed: Yes

## 7. Assessment Acceptance Criteria

| Requirement | Implementation evidence |
|---|---|
| Enrollment with available capacity | EnrollmentService and unit tests |
| Waitlist when full | FIFO queue and full-capacity tests |
| Duplicate protection | Membership checks and duplicate-request tests |
| Automatic promotion | Drop logic and FIFO promotion tests |
| Prerequisite validation | Student eligibility checks |
| Invalid request handling | Explicit operation results and tests |
| Concurrency safety | Synchronized service operations and concurrency test |
| AI-assisted development | Actual Codex prompts and development records |
| Independent code review | Manual queue/drop refactoring |
| Verification | JUnit reports and pending final personal checks |

## 8. AI Contribution and Developer Ownership

The development process involved both AI assistance and independent engineering work.

| Activity | Contribution |
|---|---|
| Initial project inspection | Candidate-directed Codex assistance |
| CLI and project setup | Candidate-directed Codex assistance |
| Enrollment service design | Candidate-defined requirements with AI assistance |
| Class separation | Candidate-directed refactoring |
| Automated tests | Candidate-defined testing goals with AI assistance |
| Drop logic debugging | Candidate-identified problem |
| ArrayDeque refactoring | Candidate's manual modification |
| Thread-safety requirement | Candidate-originated decision |
| Concurrency considerations | Candidate-originated requirement |
| Test correction | Codex assistance following manual refactoring |
| Development documentation | ChatGPT-assisted drafting, subject to candidate review |

The key distinction is that AI was used as a development assistant, not as a replacement for engineering judgment.

The implementation was guided through individual development instructions.

I identified and manually addressed a problem in the drop workflow and independently considered concurrency requirements beyond the immediate CLI demonstration.

## 9. Limitations and Future Improvements

The following limitations are recognized within the scope of this implementation.

**In-memory persistence**

Data is not preserved when the application stops.

A future implementation could introduce database persistence if required.

**Single-course scope**

The final implementation handles one course offering.

Supporting multiple courses would require expanding how course and enrollment records are managed.

**Thread safety**

Synchronization applies to an individual JVM service instance.

Multiple application instances would require coordination through a shared persistence or transaction mechanism.

**Simplified prerequisite model**

Student eligibility is represented using a Boolean status rather than an academic prerequisite service.

**Controller integration**

The business logic is designed separately from the CLI, but no actual REST controller is implemented.

These limitations are consistent with the assessment's scope boundaries.

## 10. Developer Reflection

This assessment allowed me to apply software engineering practices while using AI as a supporting tool.

I worked incrementally by directing Codex through individual instructions rather than requesting a complete implementation in one step.

Separating the application into student, course, result, and service classes helped organize the logic and make it more maintainable.

One important part of the development process was identifying a problem in the drop functionality during testing. I reviewed the implementation and manually changed the waiting-list logic to use an `ArrayDeque`, making FIFO processing clearer.

I also considered concurrency even though the application is currently a CLI. My reasoning was that the service could eventually be used by a controller handling simultaneous requests.

This led me to request thread-safe behavior and consider how to prevent multiple enrollments from exceeding course capacity.

The assessment reinforced the importance of reviewing AI-assisted code, understanding the selected data structures, and checking behavior through automated tests.

## 11. Final Summary

The project demonstrates AI-assisted development of a focused course enrollment and FIFO waitlist system using Java 25, Gradle, JUnit 5, and in-memory collections.

The development process included incremental AI-directed implementation, object-oriented refactoring, automated testing, manual debugging, and consideration of concurrent access.

A particularly important independent contribution was identifying an issue in the drop workflow and manually revising the waiting-queue implementation.

The decision to address thread safety and consider future controller-based concurrent requests was also candidate-originated.

The  project reports record 25 successful JUnit tests with no failures.

The completed work aims to demonstrate not only functional implementation, but also engineering judgment, code ownership, and responsible use of AI-assisted development tools.

---

**End of AI-Assisted Development Log**
