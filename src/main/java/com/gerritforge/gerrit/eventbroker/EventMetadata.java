// Copyright (C) 2026 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.gerrit.eventbroker;

import com.google.gerrit.common.Nullable;
import com.google.gerrit.server.util.time.TimeUtil;

/**
 * Broker metadata travelling with a message, separately from the event it carries.
 *
 * <p>The metadata is stored in the message by {@link BrokerApi#requeue(String,
 * com.google.gerrit.server.events.Event, EventMetadata)} and handed back to consumers through
 * {@link AckAwareConsumer#accept(Object, EventMetadata, MessageAcknowledgement)}. Every requeue
 * publishes a new message carrying the metadata passed to it.
 *
 * @param retry retry state of the message, {@code null} when the message was never retried
 */
public record EventMetadata(@Nullable Retry retry) {
  /** Name of the JSON field holding the metadata in a serialized message. */
  static final String JSON_FIELD = "brokerMeta";

  /** Metadata of a message that was never retried. */
  public static final EventMetadata EMPTY = new EventMetadata(null);

  /**
   * Retry state of a message whose processing failed at least once.
   *
   * @param retryCount number of failed processing attempts
   * @param lastAttemptOn epoch milliseconds of the last failed processing attempt
   * @param retryingInstanceId Gerrit instance id of the node in charge of retrying the message
   */
  public record Retry(int retryCount, long lastAttemptOn, String retryingInstanceId) {}

  /**
   * Returns whether the message processing failed at least once.
   *
   * @return {@code true} if the message carries a retry state
   */
  public boolean isRetry() {
    return retry != null;
  }

  /**
   * Returns the metadata recording one more failed processing attempt, performed now.
   *
   * @param retryingInstanceId Gerrit instance id of the node in charge of retrying the message
   * @return new metadata with an incremented retry count
   */
  public EventMetadata nextRetry(String retryingInstanceId) {
    int retryCount = retry == null ? 0 : retry.retryCount();
    return new EventMetadata(new Retry(retryCount + 1, TimeUtil.nowMs(), retryingInstanceId));
  }
}
