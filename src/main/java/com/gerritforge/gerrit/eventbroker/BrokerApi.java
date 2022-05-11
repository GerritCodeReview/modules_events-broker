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
import com.google.gerrit.server.events.Event;
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
   * <p>Call the consumer for every message received from a topic. The message is automatically
   * acknowledged if the consumer completes successfully, otherwise is put back on the topic in case
   * of any exceptions thrown.
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
   * Redeliver all stored messages for specified topic
   *
   * @param topic topic name
   */
  void replayAllEvents(String topic);
}
