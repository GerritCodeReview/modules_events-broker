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
 * A consumer that accepts a message and its acknowledgement handle, allowing for explicit
 * acknowledgement.
 *
 * @param <T> the type of the event input to the operation
 */
@FunctionalInterface
public interface AcknowledgementAwareConsumer<T> {
  /**
   * Performs this operation on the given argument.
   *
   * @param t the input argument
   * @param acknowledgement the message acknowledgement handle
   */
  void accept(T t, MessageAcknowledgement acknowledgement);
}
