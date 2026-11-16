package com.assessment.coursewaitlist;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Describes the drop and any automatic FIFO processing it caused. */
public record DropResult(Status status, Optional<String> promotedLearnerId,
                         List<String> removedIneligibleLearnerIds) {
    public enum Status { DROPPED, NOT_ENROLLED, INVALID_LEARNER_ID }

    public DropResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(promotedLearnerId, "promotedLearnerId");
        removedIneligibleLearnerIds = List.copyOf(removedIneligibleLearnerIds);
    }
}
