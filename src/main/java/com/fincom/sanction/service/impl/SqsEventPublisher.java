package com.fincom.sanction.service.impl;

import com.fincom.sanction.domain.event.Event;
import com.fincom.sanction.service.EventPublisher;

public class SqsEventPublisher implements EventPublisher {

	@Override
	public void publish(Event event) {
		// not implemented yet (NOP)
	}
}
