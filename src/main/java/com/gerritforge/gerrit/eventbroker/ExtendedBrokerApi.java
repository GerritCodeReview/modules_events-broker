// Copyright (C) 2023 The Android Open Source Project
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

import com.google.common.collect.ImmutableSet;
import com.google.gerrit.server.events.Event;
import java.util.Set;
import java.util.function.Consumer;

public interface ExtendedBrokerApi extends BrokerApi {

  /**
   * Receive asynchronously a message from a topic using a consumer's group id.
   *
   * @param topic topic name
   * @param groupId the group identifier that consumer belongs to for that topic
   * @param consumer an operation that accepts and process a single message
   */
  void receiveAsync(String topic, String groupId, Consumer<Event> consumer);

  /**
   * Get the active subscribers with their consumer's group id.
   *
   * @return {@link Set} of the topics subscribers. By default returns empty set.
   *     <p>Note: Default methods enable to add new functionality to the interfaces of the libraries
   *     and ensure binary compatibility with code written for older versions of those interfaces.
   */
  default Set<TopicSubscriberWithGroupId> topicSubscribersWithGroupId() {
    return ImmutableSet.of();
  }
}
