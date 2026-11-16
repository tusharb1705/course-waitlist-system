# Course Enrollment and FIFO Waitlist Management

A small in-memory application for a list of course offerings. Eligible learners enroll while seats are available and join a FIFO queue when full. Dropping an enrolled learner automatically promotes the first currently eligible waiter, removing any ineligible learners encountered first.

## Technology and prerequisites

- JDK 25, with `JAVA_HOME` pointing to that installation.
- Gradle 9.6.0 through the checked-in wrapper; no separate Gradle installation required.
- Java standard collections, Kotlin DSL, Jackson Databind 2.20.1 for JSON input, JUnit Jupiter 5.13.4.
- IntelliJ IDEA Ultimate is optional for running the command-line build.
- Network access to download the Gradle distribution and Maven Central dependencies on first use.

The application uses Jackson to read JSON and has no persistence. Each run starts fresh.

## Setup and commands

Open this directory as a Gradle project in IntelliJ. Set Project SDK and Gradle JVM to Java 25, then reload Gradle. Run `Main` or the commands below from the project root.

```sh
./gradlew --version
./gradlew clean test
./gradlew run
./gradlew run --args="/path/to/my-course.json"
./gradlew build
```

On Windows, use `gradlew.bat` in place of `./gradlew`. The build creates application archives under `build/distributions/`. To install locally, run `./gradlew installDist`, then run `build/install/course-waitlist-system/bin/course-waitlist-system` (or its `.bat` equivalent).

Open `build/reports/tests/test/index.html` to inspect test results. Machine-readable reports are under `build/test-results/test/`.

## Project structure

```text
build.gradle.kts                       Application, Java 25 and JUnit configuration
settings.gradle.kts                    Project name
gradle/wrapper/                        Preserved Gradle 9.6.0 wrapper
src/main/java/com/assessment/coursewaitlist/
  Course.java                          Immutable course name and positive capacity
  Student.java                         Immutable identifier and current prerequisite status
  EnrollmentService.java               Synchronized business logic and immutable state view
  EnrollmentResult.java                Enrollment outcomes
  DropResult.java                      Drop status, promotion and skipped learners
  Main.java                            JSON reader and command-line demonstration
src/main/resources/course-demo.json      Editable bundled demo input
src/test/java/com/assessment/coursewaitlist/
  EnrollmentServiceTest.java            Functional, invariant and concurrency checks
  MultiCourseEnrollmentTest.java         Multi-course isolation and map checks
  MainTest.java                         Command-line demonstration check
```

## Using the service

```java
var service = new EnrollmentService(List.of(new Course("Java", 1), new Course("Math", 2)));
service.updateStudent(new Student("learner-1", true));
var enrollment = service.enroll("Java", "learner-1"); // ENROLLED
var drop = service.drop("Java", "learner-1");        // DROPPED, no promotion
var state = service.state("Java");                   // Immutable per-course view
var enrollmentByCourse = service.enrollment();        // Immutable map
```

`updateStudent` supplies current prerequisite status. Change it with a new `Student` carrying the same identifier. Unknown learners are ineligible. Null and blank operation identifiers return an invalid-identifier result; malformed configuration/student objects throw an exception. Surrounding whitespace is stripped, and identifiers remain case-sensitive.

## JSON demonstration input

Edit [course-demo.json](src/main/resources/course-demo.json) to change the course, students, prerequisite statuses, enrollment request order or drops. Gradle copies this resource before `./gradlew run`. To keep your own scenario outside the project, save the same structure in another JSON file and pass its path with `--args`, as shown above. The installed launcher also accepts an optional JSON file path. Without a path it reads the bundled resource; rebuild the distribution after editing that resource.

```json
{
  "courses": [
    { "name": "Java Fundamentals", "capacity": 2 },
    { "name": "Algorithms", "capacity": 1 }
  ],
  "students": [
    { "id": "Alice", "prerequisiteCompleted": true },
    { "id": "Eve", "prerequisiteCompleted": false }
  ],
  "enrollmentRequests": [
    { "course": "Java Fundamentals", "student": "Alice" },
    { "course": "Java Fundamentals", "student": "Eve" },
    { "course": "Algorithms", "student": "Alice" }
  ],
  "dropRequests": [{ "course": "Java Fundamentals", "student": "Alice" }]
}
```

Course names must be unique. Each student needs a unique identifier and a literal Boolean `true` or `false`. Request arrays are processed in their listed order. A missing student is treated as ineligible. Invalid JSON fields or types produce an error instead of silently substituting values. Change eligibility in the file before running; the service's `updateStudent` method supports changes during a run when used directly.

The bundled demo includes Java Fundamentals and Algorithms. Each course has its own capacity, enrollment, and FIFO waitlist. The same eligible student can enroll in both courses.

See the [design specification](DESIGN_SPECIFICATION.md) and [test plan](TEST_PLAN.md) for review details.
