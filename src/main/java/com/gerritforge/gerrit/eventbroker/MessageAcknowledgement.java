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

/**
 * Acknowledgement handle associated with the processing of a single message.
 *
 * <p>The {@link MessageAcknowledgement} allows a consumer to explicitly acknowledge that the
 * currently processed message has been handled successfully.
 */
public interface MessageAcknowledgement {

  /**
   * Explicitly acknowledges successful processing progress for the current message.
   *
   * <p>This method performs an immediate acknowledgement attempt. Implementations are expected to
   * honour it without introducing extra batching or delay. Any batching policy belongs to the
   * caller, which can decide when to invoke {@code ack()}.
   *
   * <p>Implementations are not required to be thread-safe. Unless documented otherwise by the
   * concrete broker implementation, callers should invoke {@code ack()} from the same thread that
   * received the message and must not call it concurrently.
   *
   * <p>Calling {@code ack()} when {@link #isAutoAck()} is {@code true} will fail with {@link
   * IllegalStateException}. Failures during acknowledgement should surface as {@link
   * MessageAcknowledgementException}.
   */
  void ack();

  /**
   * Returns whether the current message is acknowledged automatically by the implementation.
   *
   * <p>When this method returns {@code true}, callers should not invoke {@link #ack()} because the
   * implementation is already handling acknowledgement automatically.
   */
  boolean isAutoAck();
}
