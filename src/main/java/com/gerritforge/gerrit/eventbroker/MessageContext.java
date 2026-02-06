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
 * Context associated with the processing of a single message.
 *
 * <p>The {@link MessageContext} allows a consumer to explicitly acknowledge that the currently
 * processed message has been handled successfully.
 *
 * <p>The effect of {@link #ack()} is implementation-specific and depends on the underlying message
 * broker. Typical behaviors may include advancing consumption progress, marking the message as
 * processed, or confirming delivery.
 *
 * <p><b>Semantics:</b>
 *
 * <ul>
 *   <li>{@code ack()} marks successful processing progress up to the current message.
 *   <li>Implementations may acknowledge messages individually or coalesce acknowledgements across
 *       multiple messages based on internal policies.
 *   <li>Multiple invocations of {@code ack()} may be treated as idempotent.
 *   <li>Failures during acknowledgement may surface as runtime exceptions.
 * </ul>
 *
 * <p><b>Threading considerations:</b> Unless otherwise documented by the broker implementation,
 * {@code ack()} is expected to be invoked from the same thread that processes the message.
 * Implementations may impose stricter or more relaxed threading requirements.
 *
 * <p>Consumers that do not require explicit acknowledgement may safely ignore this context.
 */
public interface MessageContext {

  /**
   * Explicitly acknowledges successful processing progress.
   *
   * <p>The concrete behavior is broker-specific. See the class-level documentation for semantic
   * expectations and threading considerations.
   */
  void ack();

  /**
   * Returns a no-op {@link MessageContext} implementation.
   *
   * <p>This is primarily intended for backward compatibility when a broker implementation does not
   * support explicit acknowledgement and delegates to legacy consumers.
   *
   * <p>Calling {@link #ack()} on the returned context has no effect.
   */
  static MessageContext noop() {
    return NoopMessageContext.INSTANCE;
  }

  final class NoopMessageContext implements MessageContext {
    private static final NoopMessageContext INSTANCE = new NoopMessageContext();

    private NoopMessageContext() {}

    @Override
    public void ack() {
      // intentionally no-op
    }
  }
}
