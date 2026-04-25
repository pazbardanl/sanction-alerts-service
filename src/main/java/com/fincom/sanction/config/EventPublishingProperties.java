package com.fincom.sanction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sanction.event-publishing")
public class EventPublishingProperties {

	/**
	 * Number of threads pulling from the in-memory publish queue. Must be at least 1.
	 */
	private int workerThreads = 1;

	public int getWorkerThreads() {
		return workerThreads;
	}

	public void setWorkerThreads(int workerThreads) {
		this.workerThreads = workerThreads;
	}
}
