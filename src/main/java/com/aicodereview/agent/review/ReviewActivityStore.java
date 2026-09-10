package com.aicodereview.agent.review;

import java.util.List;

public interface ReviewActivityStore {

    List<ReviewActivityEvent> getActivities(
            String reviewId);

    void clear(
            String reviewId);
}