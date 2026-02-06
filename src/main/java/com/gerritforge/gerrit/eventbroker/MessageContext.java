// Copyright (C) 2026 GerritForge, Inc.
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
 * Message context provided to broker consumers. Implementations may expose explicit commit support.
 */
public interface MessageContext {
  /** Returns true when explicit commits are supported for the current message. */
  default boolean isCommitSupported() {
    return false;
  }

  /** Commit the current message synchronously (no-op if unsupported). */
  default void commit() {}

  /** Commit the current message asynchronously (no-op if unsupported). */
  default void commitAsync() {}

  /** No-op context. */
  static MessageContext noop() {
    return NoopMessageContext.INSTANCE;
  }

  final class NoopMessageContext implements MessageContext {
    private static final NoopMessageContext INSTANCE = new NoopMessageContext();

    private NoopMessageContext() {}
  }
}
