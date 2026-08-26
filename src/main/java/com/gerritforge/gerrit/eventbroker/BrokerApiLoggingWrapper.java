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

import com.gerritforge.gerrit.eventbroker.log.MessageLogger;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.events.Event;
import com.google.inject.Inject;
import java.util.Set;

/** {@link BrokerApi} delegate that logs to the message log the events sent to the broker. */
public class BrokerApiLoggingWrapper implements BrokerApi {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private final DynamicItem<BrokerApi> apiDelegate;
  private final MessageLogger msgLog;

  @Inject
  public BrokerApiLoggingWrapper(DynamicItem<BrokerApi> apiDelegate, MessageLogger msgLog) {
    this.apiDelegate = apiDelegate;
    this.msgLog = msgLog;
  }

  @Override
  public ListenableFuture<Boolean> send(String topic, Event message) {
    return send(topic, message, MessageLogger.Direction.PUBLISH);
  }

  protected ListenableFuture<Boolean> send(
      String topic, Event message, MessageLogger.Direction direction) {
    ListenableFuture<Boolean> resultF = delegate().send(topic, message);
    Futures.addCallback(
        resultF,
        new FutureCallback<Boolean>() {
          @Override
          public void onSuccess(Boolean result) {
            if (result) {
              msgLog.log(direction, topic, message);
            }
          }

          @Override
          public void onFailure(Throwable throwable) {
            log.atSevere().withCause(throwable).log(
                "Failed to %s message '%s' to topic '%s'", direction, message, topic);
          }
        },
        MoreExecutors.directExecutor());

    return resultF;
  }

  private BrokerApi delegate() {
    return apiDelegate.get();
  }

  @Override
  public void receiveAsync(String topic, AckAwareConsumer<Event> consumer) {
    delegate().receiveAsync(topic, logged(topic, consumer));
  }

  @Override
  public void receiveAsync(String topic, String groupId, AckAwareConsumer<Event> consumer) {
    delegate().receiveAsync(topic, groupId, logged(topic, consumer));
  }

  @Override
  public void receiveAsyncWithPartition(
      String topic, String partition, String groupId, AckAwareConsumer<Event> consumer) {
    delegate().receiveAsyncWithPartition(topic, partition, groupId, logged(topic, consumer));
  }

  private AckAwareConsumer<Event> logged(String topic, AckAwareConsumer<Event> consumer) {
    return (event, acknowledgement) -> {
      msgLog.log(MessageLogger.Direction.CONSUME, topic, event);
      consumer.accept(event, acknowledgement);
    };
  }

  @Override
  public void disconnect() {
    delegate().disconnect();
  }

  @Override
  public void disconnect(String topic, @Nullable String groupId) {
    delegate().disconnect(topic, groupId);
  }

  @Override
  public Set<TopicSubscriber> topicSubscribers() {
    return delegate().topicSubscribers();
  }

  @Override
  public Set<TopicSubscriberWithGroupId> topicSubscribersWithGroupId() {
    return delegate().topicSubscribersWithGroupId();
  }

  @Override
  public void replayAllEvents(String topic) {
    delegate().replayAllEvents(topic);
  }

  @Override
  public boolean isAutoAck() {
    return delegate().isAutoAck();
  }
}
