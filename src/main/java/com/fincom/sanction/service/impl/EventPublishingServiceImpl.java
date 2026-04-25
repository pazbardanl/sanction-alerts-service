package com.fincom.sanction.service.impl;

import com.fincom.sanction.config.EventPublishingProperties;
import com.fincom.sanction.domain.event.Event;
import com.fincom.sanction.domain.event.PublishEventRequest;
import com.fincom.sanction.domain.event.PublishMethod;
import com.fincom.sanction.mapper.SanctionAlertsMapper;
import com.fincom.sanction.service.EventPublisher;
import com.fincom.sanction.service.EventPublishingService;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EventPublishingServiceImpl implements EventPublishingService {

	private static final Logger log = LoggerFactory.getLogger(EventPublishingServiceImpl.class);

	private final BlockingQueue<PublishEventRequest> queue;
	private final Map<PublishMethod, EventPublisher> publishers;
	private final EventPublishingProperties properties;
	private final SanctionAlertsMapper mapper;
	private final ExecutorService workers;

	public EventPublishingServiceImpl(
			BlockingQueue<PublishEventRequest> queue,
			Map<PublishMethod, EventPublisher> publishers,
			EventPublishingProperties properties,
			SanctionAlertsMapper mapper) {
		this.queue = queue;
		this.publishers = publishers;
		this.properties = properties;
		this.mapper = mapper;
		int numOfWorkers = Math.max(1, properties.getWorkerThreads());
		AtomicInteger seq = new AtomicInteger();
		ThreadFactory factory =
				r -> {
					Thread t = new Thread(r, "event-publish-" + seq.incrementAndGet());
					t.setDaemon(true);
					return t;
				};
		this.workers = Executors.newFixedThreadPool(numOfWorkers, factory);
		for (int i = 0; i < numOfWorkers; i++) {
			workers.submit(this::workerLoop);
		}
		log.info("Event publishing started with {} worker thread(s)", numOfWorkers);
	}

	@Override
	public void publishEventAsync(PublishEventRequest request) {
		boolean enqueued = queue.offer(request);
		logIfEnqueueRefusedOutOfMemoryThrowsElsewhere(enqueued, request);
	}

	private void logIfEnqueueRefusedOutOfMemoryThrowsElsewhere(
			boolean enqueued, PublishEventRequest request) {
		if (!enqueued) {
			log.error("Failed to queue publish request (queue full?); request={}", request);
		}
	}

	@PreDestroy
    public
	void shutdown() {
		workers.shutdown();
		try {
			if (!workers.awaitTermination(15, TimeUnit.SECONDS)) {
				workers.shutdownNow();
			}
		} catch (InterruptedException e) {
			workers.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	private void workerLoop() {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				PublishEventRequest request = queue.take();
				publishRequest(request);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public void publishRequest(PublishEventRequest request) {
		try {
			Event event = mapper.toEvent(request);
			EventPublisher publisher = resolvePublisher(request.publishMethod());
			publisher.publish(event);
		} catch (Exception e) {
			log.error("Failed to publish event; request={}", request, e);
		}
	}

	private EventPublisher resolvePublisher(PublishMethod method) {
		EventPublisher publisher = publishers.get(method);
		if (publisher == null) {
			log.warn("No publisher for {}, falling back to STDOUT", method);
			publisher = publishers.get(PublishMethod.STDOUT);
		}
		if (publisher == null) {
			throw new IllegalStateException("No STDOUT event publisher available");
		}
		return publisher;
	}
}
