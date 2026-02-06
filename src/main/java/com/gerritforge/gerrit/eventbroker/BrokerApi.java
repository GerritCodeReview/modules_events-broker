// Copyright (C) 2019 The Android Open Source Project
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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.server.events.Event;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

/** API for sending/receiving events through a message Broker. */
public interface BrokerApi {

  /**
   * Send a message to a topic.
   *
   * @param topic topic name
   * @param message to be send to the topic
   * @return a future that returns when the message has been sent.
   */
  ListenableFuture<Boolean> send(String topic, Event message);

  /**
   * Receive asynchronously a message from a topic.
   *
   * @param topic topic name
   * @param consumer an operation that accepts and process a single message
   */
  void receiveAsync(String topic, Consumer<Event> consumer);

  /**
   * Get the active subscribers
   *
   * @return {@link Set} of the topics subscribers
   */
  Set<TopicSubscriber> topicSubscribers();

  /** Disconnect from broker and cancel all active consumers */
  void disconnect();

  /**
   * Disconnect from broker and cancel all active consumers on the specified topic.
   *
   * @param topic topic name of the consumers to cancel
   * @param groupId when not null, filter the consumers to cancel by groupId
   */
  void disconnect(String topic, @Nullable String groupId);

  /**
   * Redeliver all stored messages for specified topic
   *
   * @param topic topic name
   */
  void replayAllEvents(String topic);

  /**
   * Receive asynchronously a message from a topic using a consumer's group id.
   *
   * @param topic topic name
   * @param groupId the group identifier that consumer belongs to for that topic
   * @param consumer an operation that accepts and process a single message
   * @since 3.10
   */
  void receiveAsync(String topic, String groupId, Consumer<Event> consumer);

  /**
   * Get the active subscribers with their consumer's group id.
   *
   * @return {@link Set} of the topics subscribers using a consumer's group id.
   * @since 3.10
   */
  Set<TopicSubscriberWithGroupId> topicSubscribersWithGroupId();

  /**
   * Receive messages asynchronously using a context-aware consumer.
   *
   * <p><b>Backwards compatibility:</b> The default implementation delegates to the legacy {@link
   * #receiveAsync(String, Consumer)} by wrapping the provided {@link ContextAwareConsumer} and
   * passing a {@link MessageContext#noop()}. As a consequence, registrations done through this
   * default method are tracked as legacy {@link TopicSubscriber}s and will <b>not</b> appear in
   * {@link #topicSubscribersWithContext()} (or {@link #topicSubscribersWithContextAndGroupId()}).
   *
   * <p>Broker implementations that support explicit acknowledgements/commits should override this
   * method to:
   *
   * <ul>
   *   <li>provide a real {@link MessageContext} instance, and
   *   <li>track the subscription via {@link #topicSubscribersWithContext()} (and group-id variant).
   * </ul>
   *
   * @param topic topic name
   * @param consumer an operation that accepts and processes a single message with context
   */
  default void receiveAsyncWithContext(String topic, ContextAwareConsumer<Event> consumer) {
    receiveAsync(topic, event -> consumer.accept(event, MessageContext.noop()));
  }

  /**
   * Get the active subscribers that registered a context-aware consumer.
   *
   * @return {@link Set} of the topics subscribers using a consumer with context
   * @since 3.14
   */
  default Set<TopicSubscriberWithContext> topicSubscribersWithContext() {
    return Collections.emptySet();
  }

  /**
   * Get active context-aware subscribers along with group id.
   *
   * @return {@link Set} of the topics subscribers using a consumer's group id with context.
   * @since 3.14
   */
  default Set<TopicSubscriberWithContextWithGroupId> topicSubscribersWithContextAndGroupId() {
    return Collections.emptySet();
  }

  /**
   * Receive messages asynchronously using a context-aware consumer and a consumer group id.
   *
   * <p><b>Backwards compatibility:</b> The default implementation delegates to the legacy {@link
   * #receiveAsync(String, String, Consumer)} by wrapping the provided {@link ContextAwareConsumer}
   * and passing a {@link MessageContext#noop()}. As a result, subscriptions registered through this
   * default method are tracked as legacy {@link TopicSubscriberWithGroupId} entries and will
   * <b>not</b> appear in {@link #topicSubscribersWithContextAndGroupId()} (or {@link
   * #topicSubscribersWithContext()}).
   *
   * <p>Broker implementations that support explicit acknowledgements/commits should override this
   * method to:
   *
   * <ul>
   *   <li>provide a real {@link MessageContext} instance, and
   *   <li>register the subscription via {@link #topicSubscribersWithContextAndGroupId()}.
   * </ul>
   *
   * @param topic topic name
   * @param groupId the group identifier that the consumer belongs to for that topic
   * @param consumer an operation that accepts and processes a single message with context
   */
  default void receiveAsyncWithContext(
      String topic, String groupId, ContextAwareConsumer<Event> consumer) {
    receiveAsync(topic, groupId, event -> consumer.accept(event, MessageContext.noop()));
  }
}
