package com.fincom.sanction.service;

import com.fincom.sanction.domain.event.PublishEventRequest;

public interface EventPublishingService {
    void publishEventAsync(PublishEventRequest request);
}
