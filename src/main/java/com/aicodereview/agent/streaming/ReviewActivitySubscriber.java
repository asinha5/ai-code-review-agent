package com.aicodereview.agent.streaming;

import com.aicodereview.agent.activity.ReviewActivityEvent;

public interface ReviewActivitySubscriber {

    void onActivity(ReviewActivityEvent event);
}