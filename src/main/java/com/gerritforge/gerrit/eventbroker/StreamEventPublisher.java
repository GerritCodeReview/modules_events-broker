// Copyright (C) 2021 The Android Open Source Project
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

import com.gerritforge.gerrit.eventbroker.executor.StreamEventPublisherExecutor;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

@Singleton
public class StreamEventPublisher implements EventListener {
  public static final String STREAM_EVENTS_TOPIC = "stream_events_topic";
  private final DynamicItem<BrokerApi> brokerApiDynamicItem;
  private final String streamEventsTopic;
  private final Executor executor;
  private final String instanceId;

  @Inject
  public StreamEventPublisher(
      DynamicItem<BrokerApi> brokerApi,
      @Named(STREAM_EVENTS_TOPIC) String streamEventsTopic,
      @StreamEventPublisherExecutor Executor executor,
      @Nullable @GerritInstanceId String instanceId) {
    this.brokerApiDynamicItem = brokerApi;
    this.streamEventsTopic = streamEventsTopic;
    this.executor = executor;
    this.instanceId = instanceId;
  }

  @Override
  public void onEvent(Event event) {
    executor.execute(new EventTask(event, brokerApiDynamicItem.get(), instanceId));
  }

  class EventTask implements Runnable {
    private final Event event;
    private final BrokerApi brokerApi;
    private final String instanceId;

    EventTask(Event event, BrokerApi brokerApi, String instanceId) {
      this.event = event;
      this.brokerApi = brokerApi;
      this.instanceId = instanceId;
    }

    @Override
    public void run() {
      if (brokerApi != null && shouldSend(event)) {
        brokerApi.send(streamEventsTopic, event);
      }
    }

    private boolean shouldSend(Event event) {
      return (instanceId == null && event.instanceId == null)
          || (instanceId != null && instanceId.equals(event.instanceId));
    }

    @Override
    public String toString() {
      return String.format("Send event '%s' to target instance", event.type);
    }
  }
}
