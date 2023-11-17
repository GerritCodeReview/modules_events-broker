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

import com.gerritforge.gerrit.eventbroker.log.MessageLogger;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.events.Event;
import com.google.inject.Inject;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventsBrokerApiWrapper implements BrokerApi {
  private static final Logger log = LoggerFactory.getLogger(EventsBrokerApiWrapper.class);
  private final Executor executor;
  private final DynamicItem<BrokerApi> apiDelegate;
  private final MessageLogger msgLog;

  @Inject
  public EventsBrokerApiWrapper(
      @EventsBrokerExecutor Executor executor,
      DynamicItem<BrokerApi> apiDelegate,
      MessageLogger msgLog) {
    this.apiDelegate = apiDelegate;
    this.executor = executor;
    this.msgLog = msgLog;
  }

  public boolean sendSync(String topic, Event event) {
    try {
      return send(topic, event).get();
    } catch (Throwable e) {
      return false;
    }
  }

  @Override
  public ListenableFuture<Boolean> send(String topic, Event message) {
    ListenableFuture<Boolean> resfultF = apiDelegate.get().send(topic, message);
    Futures.addCallback(
        resfultF,
        new FutureCallback<Boolean>() {
          @Override
          public void onSuccess(Boolean result) {
            if (result) {
              msgLog.log(MessageLogger.Direction.PUBLISH, topic, message);
            }
          }

          @Override
          public void onFailure(Throwable throwable) {
            log.error(
                "Failed to publish message '{}' to topic '{}' - error: {}",
                message,
                topic,
                throwable.getMessage());
          }
        },
        executor);

    return resfultF;
  }

  @Override
  public void receiveAsync(String topic, Consumer<Event> messageConsumer) {
    apiDelegate.get().receiveAsync(topic, messageConsumer);
  }

  @Override
  public void disconnect() {
    apiDelegate.get().disconnect();
  }

  @Override
  public Set<TopicSubscriber> topicSubscribers() {
    return apiDelegate.get().topicSubscribers();
  }

  @Override
  public void replayAllEvents(String topic) {
    apiDelegate.get().replayAllEvents(topic);
  }
}
