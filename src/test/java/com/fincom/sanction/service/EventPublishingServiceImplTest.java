package com.fincom.sanction.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fincom.sanction.domain.alert.AlertStatus;
import com.fincom.sanction.domain.event.Event;
import com.fincom.sanction.domain.event.EventType;
import com.fincom.sanction.domain.event.PublishEventRequest;
import com.fincom.sanction.domain.event.PublishMethod;
import com.fincom.sanction.mapper.SanctionAlertsMapperImpl;
import com.fincom.sanction.service.impl.EventPublishingServiceImpl;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class EventPublishingServiceImplTest {

	private final SanctionAlertsMapperImpl mapper = new SanctionAlertsMapperImpl();

	@Test
	void toEvent_mapsTypeNameAndFields() {
		UUID id = UUID.randomUUID();
		LocalDateTime t = LocalDateTime.parse("2026-04-20T10:00:00");
		PublishEventRequest req =
				new PublishEventRequest(
						id, "tenant-1", EventType.ALERT_DECIDED, PublishMethod.STDOUT, AlertStatus.CLEARED, t);
		Event e = mapper.toEvent(req);
		assertThat(e.alertId()).isEqualTo(id);
		assertThat(e.tenantId()).isEqualTo("tenant-1");
		assertThat(e.eventType()).isEqualTo(EventType.ALERT_DECIDED.getName());
		assertThat(e.status()).isEqualTo(AlertStatus.CLEARED);
		assertThat(e.timestamp()).isEqualTo(t);
	}

	@Test
	void publishRequest_invokesPublisherForMethod() {
		BlockingQueue<PublishEventRequest> q = new ArrayBlockingQueue<>(8);
		Event[] captured = new Event[1];
		Map<PublishMethod, EventPublisher> publishers = new EnumMap<>(PublishMethod.class);
		publishers.put(
				PublishMethod.STDOUT,
				event -> {
					captured[0] = event;
				});
		com.fincom.sanction.config.EventPublishingProperties props = new com.fincom.sanction.config.EventPublishingProperties();
		props.setWorkerThreads(1);
		EventPublishingServiceImpl svc = new EventPublishingServiceImpl(q, publishers, props, mapper);
		PublishEventRequest req =
				new PublishEventRequest(
						UUID.randomUUID(),
						"tenant-1",
						EventType.ALERT_DECIDED,
						PublishMethod.STDOUT,
						AlertStatus.CLEARED,
						LocalDateTime.now());
		svc.publishRequest(req);
		assertThat(captured[0].eventType()).isEqualTo(EventType.ALERT_DECIDED.getName());
		svc.shutdown();
	}

	@Test
	void queueThenWorker_publishesAsync() throws Exception {
		BlockingQueue<PublishEventRequest> q = new ArrayBlockingQueue<>(8);
		Event[] captured = new Event[1];
		CompletableFuture<Void> first = new CompletableFuture<>();
		Map<PublishMethod, EventPublisher> publishers = new EnumMap<>(PublishMethod.class);
		publishers.put(
				PublishMethod.STDOUT,
				e -> {
					captured[0] = e;
					first.complete(null);
				});
		com.fincom.sanction.config.EventPublishingProperties props = new com.fincom.sanction.config.EventPublishingProperties();
		props.setWorkerThreads(1);
		EventPublishingServiceImpl svc = new EventPublishingServiceImpl(q, publishers, props, mapper);
		PublishEventRequest req =
				new PublishEventRequest(
						UUID.randomUUID(),
						"tenant-1",
						EventType.ALERT_DECIDED,
						PublishMethod.STDOUT,
						AlertStatus.CLEARED,
						LocalDateTime.now());
		svc.publishEventAsync(req);
		first.get(2, TimeUnit.SECONDS);
		assertThat(captured[0].tenantId()).isEqualTo("tenant-1");
		svc.shutdown();
	}
}
