/*
 * Copyright (c) 2011-2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.vertx.core.application.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ShutdownHook implements Runnable {

  private final Vertx vertx;
  private final Consumer<AsyncResult<Void>> whenComplete;

  public ShutdownHook(Vertx vertx, Consumer<AsyncResult<Void>> whenComplete) {
    this.vertx = vertx;
    this.whenComplete = whenComplete;
  }

  @Override
  public void run() {
    try {
      whenComplete.accept(closeVertx());
    } catch (ExecutionException e) {
      whenComplete.accept(Future.failedFuture(e.getCause()));
    } catch (TimeoutException e) {
      whenComplete.accept(null);
    }
  }

  private AsyncResult<Void> closeVertx() throws ExecutionException, TimeoutException {
    CompletableFuture<Void> future = vertx.close().toCompletionStage().toCompletableFuture();
    long remaining = Duration.ofMinutes(2).toMillis();
    long stop = System.currentTimeMillis() + remaining;
    boolean interrupted = false;
    while (true) {
      try {
        if (remaining >= 0) {
          future.get(remaining, MILLISECONDS);
          return Future.succeededFuture();
        } else {
          return null;
        }
      } catch (InterruptedException e) {
        interrupted = true;
        remaining = stop - System.currentTimeMillis();
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}
