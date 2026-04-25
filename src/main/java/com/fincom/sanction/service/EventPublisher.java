package com.fincom.sanction.service;

import com.fincom.sanction.domain.event.Event;

public interface EventPublisher {

	void publish(Event event);
}
