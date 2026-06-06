package com.gather.repository;

import com.google.api.core.ApiFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Awaits a Firestore {@link ApiFuture} with a bounded timeout so a slow or unreachable Firestore
 * can never block a thread indefinitely (audit finding H3). A timeout is surfaced as an
 * {@link ExecutionException} so existing {@code catch (ExecutionException)} handlers cover it.
 */
public final class FirestoreAwait {

    /** Max time to wait for any single Firestore operation. */
    static final long TIMEOUT_SECONDS = 30;

    private FirestoreAwait() {
    }

    public static <T> T get(ApiFuture<T> future) throws InterruptedException, ExecutionException {
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new ExecutionException("Firestore operation timed out after " + TIMEOUT_SECONDS + "s", e);
        }
    }
}
