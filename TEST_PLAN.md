# Verification plan

Run `./gradlew clean test` using JDK 25. Tests exercise the service directly without console interaction. Expected results and FIFO orders are explicitly asserted rather than derived from the implementation. `assertState` checks exact membership/order, capacity, uniqueness and disjointness at important transitions.

| Requirement | Test method(s) |
| --- | --- |
| Available seat and full course | `enrollsWhenSeatAvailableAndQueuesWhenFull` |
| Duplicate enrollment and waiting entries | `duplicateRequestsDoNotChangeStateOrQueueOrder` |
| FIFO across drops | `promotesInFifoOrderAcrossMultipleDrops` |
| Missing prerequisite, unknown learner, full course rejection | `rejectsIneligibleAndUnknownLearners`, `rejectsIneligibleLearnerEvenWhenFull` |
| Newly ineligible queue heads | `removesNewlyIneligibleFrontLearnersAndPromotesNextEligible` |
| Eligibility changes and restoration | `usesLatestEligibilityWhenLearnerBecomesEligibleAgain` |
| Entire queue becomes ineligible | `removesEntireIneligibleQueueAndLeavesSeatAvailable` |
| Empty queue and reenrollment | `dropsWithEmptyQueueAndAllowsReenrollment` |
| Unknown, waiting and repeated drop requests | `nonEnrolledAndRepeatedDropsCannotRemoveWaitersOrPromote` |
| Null, empty and whitespace identifiers | `invalidOperationIdentifiersLeaveStateUnchanged` (five cases) |
| Identifier normalization and case | `stripsSurroundingWhitespaceAndKeepsIdentifiersCaseSensitive` |
| Positive capacity and valid model construction | `rejectsInvalidConfigurationAndStudentRecords` |
| Defensive immutable views and results | `snapshotsAreImmutableAndDoNotChangeAfterLaterOperations` |
| Concurrent requests for the final seat | `concurrentRequestsForFinalSeatNeverExceedCapacity` |
| Capacity, uniqueness and no overlap | Shared `assertState` checks throughout |
| Multiple courses, per-course capacity and immutable enrollment map | `MultiCourseEnrollmentTest` |
| Bundled JSON demonstration | `MainTest.demonstrationShowsEnrollmentRejectionsPromotionAndFinalState` |
| Custom JSON student and prerequisite values | `MainTest.readsCustomStudentsPrerequisitesAndRequests` |
| Invalid prerequisite field type | `MainTest.rejectsMalformedPrerequisiteValues` |

The concurrency test fills all but one seat, starts two workers with ready/start `CountDownLatch` coordination, and collects both results with bounded waits. It asserts exactly one enrollment and one waitlist result, verifies both learners remain represented, and checks capacity and disjointness. It intentionally does not assume which thread wins. There are no arbitrary sleeps.

The test suite is focused evidence, not an exhaustive proof of all thread interleavings. The shared-monitor design provides the reason that each state transition is atomic.

`MainTest` runs the real entry point with bundled and custom JSON. It checks the printed outcomes for enrollment, prerequisite rejection, promotion and final state. For a manual check, run `./gradlew run` and inspect the same output. Review the production implementation and tests separately before assessment submission.
