package com.fincom.sanction.service;

import com.fincom.sanction.config.EventPublishingProperties;
import com.fincom.sanction.domain.event.PublishEventRequest;
import com.fincom.sanction.domain.event.PublishMethod;
import com.fincom.sanction.service.impl.KafkaEventPublisher;
import com.fincom.sanction.service.impl.SqsEventPublisher;
import com.fincom.sanction.service.impl.StdoutEventPublisher;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EventPublishingProperties.class)
class EventPublishingConfiguration {

	@Bean
	BlockingQueue<PublishEventRequest> eventPublishRequestQueue() {
		return new LinkedBlockingQueue<>();
	}

	@Bean
	StdoutEventPublisher stdoutEventPublisher() {
		return new StdoutEventPublisher();
	}

	@Bean
	KafkaEventPublisher kafkaEventPublisher() {
		return new KafkaEventPublisher();
	}

	@Bean
	SqsEventPublisher sqsEventPublisher() {
		return new SqsEventPublisher();
	}

	@Bean
	Map<PublishMethod, EventPublisher> eventPublishers(
			StdoutEventPublisher stdout, KafkaEventPublisher kafka, SqsEventPublisher sqs) {
		Map<PublishMethod, EventPublisher> map = new EnumMap<>(PublishMethod.class);
		map.put(PublishMethod.STDOUT, stdout);
		map.put(PublishMethod.KAFKA, kafka);
		map.put(PublishMethod.SQS, sqs);
		return map;
	}
}
