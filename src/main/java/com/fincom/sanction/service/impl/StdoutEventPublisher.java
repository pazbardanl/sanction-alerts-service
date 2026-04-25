package com.fincom.sanction.service.impl;

import com.fincom.sanction.domain.event.Event;
import com.fincom.sanction.service.EventPublisher;

public class StdoutEventPublisher implements EventPublisher {

	@Override
	public void publish(Event event) {
		System.out.println(event);
	}
}
