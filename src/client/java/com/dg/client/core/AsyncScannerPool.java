package com.dg.client.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bounded background worker pool for heavy chunk snapshots and anti-decoy statistical analysis.
 * Workers operate solely on immutable/copied snapshots and never access Blaze3D or mutable world state.
 */
public final class AsyncScannerPool {
    private final ExecutorService executor;

    public AsyncScannerPool() {
        int threads = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
        AtomicInteger counter = new AtomicInteger(1);
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                threads,
                threads,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(512),
                r -> {
                    Thread t = new Thread(r, "DGClient-Scanner-" + counter.getAndIncrement());
                    t.setDaemon(true);
                    t.setPriority(Thread.NORM_PRIORITY - 1);
                    return t;
                },
                new ThreadPoolExecutor.DiscardOldestPolicy()
        );
        this.executor = pool;
    }

    public void submit(Runnable task) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                DGLogger.error("Background scanner task failed", t);
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
