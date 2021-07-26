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

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gerrit.server.events.Event;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class InProcessBrokerApi implements BrokerApi {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private static final Integer DEFAULT_MESSAGE_QUEUE_SIZE = 100;
  private static final String STREAM_EVENTS_TOPIC = "gerrit";

  private final Map<String, EvictingQueue<Event>> messagesQueueMap;
  private final Map<String, EventBus> eventBusMap;
  private final Set<TopicSubscriber> topicSubscribers;

  public InProcessBrokerApi() {
    this.eventBusMap = new MapMaker().concurrencyLevel(1).makeMap();
    this.messagesQueueMap = new MapMaker().concurrencyLevel(1).makeMap();
    this.topicSubscribers = new HashSet<>();
  }

  @Override
  public ListenableFuture<Boolean> send(String topic, Event message) {
    EventBus topicEventConsumers = eventBusMap.get(topic);
    SettableFuture<Boolean> future = SettableFuture.create();

    if (topicEventConsumers != null) {
      topicEventConsumers.post(message);
      future.set(true);
    } else {
      future.set(false);
    }

    return future;
  }

  @Override
  public void receiveAsync(String topic, Consumer<Event> eventConsumer) {
    EventBus topicEventConsumers = eventBusMap.get(topic);
    if (topicEventConsumers == null) {
      topicEventConsumers = new EventBus(topic);
      eventBusMap.put(topic, topicEventConsumers);
    }

    topicEventConsumers.register(eventConsumer);
    topicSubscribers.add(topicSubscriber(topic, eventConsumer));

    EvictingQueue<Event> messageQueue = EvictingQueue.create(DEFAULT_MESSAGE_QUEUE_SIZE);
    messagesQueueMap.put(topic, messageQueue);
    topicEventConsumers.register(new EventBusMessageRecorder(messageQueue));
  }

  @Override
  public Set<TopicSubscriber> topicSubscribers() {
    return ImmutableSet.copyOf(topicSubscribers);
  }

  @Override
  public void disconnect() {
    this.eventBusMap.clear();
  }

  @Override
  public void replayAllEvents(String topic) {
    if (messagesQueueMap.containsKey(topic)) {
      messagesQueueMap.get(topic).stream().forEach(eventMessage -> send(topic, eventMessage));
    }
  }

  @Override
  public String getStreamEventsTopic() {
    return STREAM_EVENTS_TOPIC;
  }

  private static class EventBusMessageRecorder {
    private EvictingQueue messagesQueue;

    public EventBusMessageRecorder(EvictingQueue messagesQueue) {
      this.messagesQueue = messagesQueue;
    }

    @Subscribe
    public void recordCustomerChange(Event e) {
      if (!messagesQueue.contains(e)) {
        messagesQueue.add(e);
      }
    }
  }
}
