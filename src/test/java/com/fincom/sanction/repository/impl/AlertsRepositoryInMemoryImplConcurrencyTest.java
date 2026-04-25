package com.fincom.sanction.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.fincom.sanction.domain.alert.Alert;
import com.fincom.sanction.domain.alert.AlertStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlertsRepositoryInMemoryImplConcurrencyTest {

	private AlertsRepositoryInMemoryImpl repository;

	@BeforeEach
	void setUp() {
		repository = new AlertsRepositoryInMemoryImpl();
	}

	@Test
	void manyConcurrentUpdatesOnSingleAlert_allCompleteWithConsistentState() throws Exception {
		UUID id = UUID.randomUUID();
		String tenant = "tenant-stress";
		LocalDateTime t = LocalDateTime.parse("2026-05-01T10:00:00");
		repository.storeAlert(
				new Alert(
						id, "tx", "ent", 0.5f, AlertStatus.OPEN, null, tenant, t, t, null));

		int nThreads = 8;
		int updatesPerThread = 40;
		ExecutorService pool = Executors.newFixedThreadPool(nThreads);
		CyclicBarrier start = new CyclicBarrier(nThreads);

		for (int ti = 0; ti < nThreads; ti++) {
			final int tid = ti;
			pool.submit(
					() -> {
						try {
							start.await();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							throw new RuntimeException(e);
						} catch (java.util.concurrent.BrokenBarrierException e) {
							throw new RuntimeException(e);
						}
						for (int j = 0; j < updatesPerThread; j++) {
							repository.updateAlertStatusAndDecisionNote(
									tenant, id, AlertStatus.CLEARED, "n-" + tid + "-" + j);
						}
					});
		}
		pool.shutdown();
		assertThat(pool.awaitTermination(1, TimeUnit.MINUTES))
				.as("all concurrent updates should finish")
				.isTrue();

		Alert a = repository.getAlert(tenant, id);
		assertThat(a).isNotNull();
		assertThat(a.status()).isEqualTo(AlertStatus.CLEARED);
		assertThat(a.decisionNote())
				.as("last writer left one of the update strings")
				.contains("-");
	}

	@Test
	void twoThreadsTightContention_bothSucceed() throws Exception {
		UUID id = UUID.randomUUID();
		String tenant = "tenant-barrier";
		LocalDateTime t = LocalDateTime.parse("2026-05-01T10:00:00");
		repository.storeAlert(
				new Alert(
						id, "tx", "ent", 0.5f, AlertStatus.OPEN, null, tenant, t, t, null));

		CyclicBarrier gate = new CyclicBarrier(2);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		Throwable[] errors = new Throwable[2];
		Runnable t1 =
				() -> {
					try {
						gate.await();
						repository.updateAlertStatusAndDecisionNote(
								tenant, id, AlertStatus.CLEARED, "t1");
					} catch (Throwable e) {
						synchronized (errors) {
							errors[0] = e;
						}
					}
				};
		Runnable t2 =
				() -> {
					try {
						gate.await();
						repository.updateAlertStatusAndDecisionNote(
								tenant, id, AlertStatus.CONFIRMED_HIT, "t2");
					} catch (Throwable e) {
						synchronized (errors) {
							errors[1] = e;
						}
					}
				};

		try {
			pool.submit(t1);
			pool.submit(t2);
		} finally {
			pool.shutdown();
		}
		assertThat(pool.awaitTermination(1, TimeUnit.MINUTES))
				.as("concurrent decision updates on same key")
				.isTrue();
		if (errors[0] != null) {
			throw new AssertionError(errors[0]);
		}
		if (errors[1] != null) {
			throw new AssertionError(errors[1]);
		}
		Alert a = repository.getAlert(tenant, id);
		assertThat(a)
				.as("one update applies after the other, both to same alert row")
				.isNotNull();
		assertThat(
						(a.status() == AlertStatus.CLEARED && "t1".equals(a.decisionNote()))
								|| (a.status() == AlertStatus.CONFIRMED_HIT
										&& "t2".equals(a.decisionNote())))
				.as("last writer is always a whole decision (status matches note for that pass)")
				.isTrue();
	}
}
