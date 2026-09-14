package com.aicodereview.agent.activity;

import java.util.List;

public interface ReviewActivityStore {

    void add(ReviewActivityEvent event);

    List<ReviewActivityEvent> getActivities(String reviewId);

    void clear(String reviewId);
}