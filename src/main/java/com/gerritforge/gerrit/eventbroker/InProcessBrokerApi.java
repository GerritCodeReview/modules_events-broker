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

import static com.gerritforge.gerrit.eventbroker.TopicSubscriber.topicSubscriber;
import static com.gerritforge.gerrit.eventbroker.TopicSubscriberWithGroupId.topicSubscriberWithGroupId;

import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gerrit.server.events.Event;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class InProcessBrokerApi implements ExtendedBrokerApi {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private final Set<TopicSubscriber> topicSubscribers;
  private final Set<TopicSubscriberWithGroupId> topicSubscribersWithGroupId;

  public InProcessBrokerApi() {
    this.topicSubscribers = new HashSet<>();
    this.topicSubscribersWithGroupId = new HashSet<>();
  }

  @Override
  public ListenableFuture<Boolean> send(String topic, Event message) {
    return unsupported();
  }

  @Override
  public void receiveAsync(String topic, Consumer<Event> eventConsumer) {
<<<<<<< PATCH SET (3dc168 Provide subscribers with consumer's group id)
    receiveAsync(
        topic, eventConsumer, () -> topicSubscribers.add(topicSubscriber(topic, eventConsumer)));
  }

  /*
   This implementation is not design to deal with the group ids.
   All events will be delivered to all subscribers.
  */
  @Override
  public void receiveAsync(String topic, String groupId, Consumer<Event> eventConsumer) {
    receiveAsync(
        topic,
        eventConsumer,
        () ->
            topicSubscribersWithGroupId.add(
                topicSubscriberWithGroupId(groupId, topicSubscriber(topic, eventConsumer))));
=======
    topicSubscribers.add(topicSubscriber(topic, eventConsumer));
>>>>>>> BASE      (f7213e Remove actual implementation of InProcessBrokerApi)
  }

  @Override
  public Set<TopicSubscriber> topicSubscribers() {
    return ImmutableSet.copyOf(topicSubscribers);
  }

  @Override
  public Set<TopicSubscriberWithGroupId> topicSubscribersWithGroupId() {
    return ImmutableSet.copyOf(topicSubscribersWithGroupId);
  }

  @Override
  public void disconnect() {
    this.topicSubscribers.clear();
  }

  @Override
  public void replayAllEvents(String topic) {
    unsupported();
  }

  private <T> T unsupported() {
    throw new UnsupportedOperationException(
        "InProcessBrokerApi is not intended to be used as a real broker");
  }

  private void receiveAsync(
      String topic, Consumer<Event> eventConsumer, Runnable addTopicSubscriber) {
    EventBus topicEventConsumers = eventBusMap.get(topic);
    if (topicEventConsumers == null) {
      topicEventConsumers = new EventBus(topic);
      eventBusMap.put(topic, topicEventConsumers);
    }

    topicEventConsumers.register(eventConsumer);
    addTopicSubscriber.run();

    EvictingQueue<Event> messageQueue = EvictingQueue.create(DEFAULT_MESSAGE_QUEUE_SIZE);
    messagesQueueMap.put(topic, messageQueue);
    topicEventConsumers.register(new EventBusMessageRecorder(messageQueue));
  }
}
