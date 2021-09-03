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

package com.gerritforge.gerrit.eventbroker.publisher;

import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.gerritforge.gerrit.eventbroker.metrics.BrokerMetrics;
import com.gerritforge.gerrit.eventbroker.publisher.executor.StreamEventPublisherExecutor;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Singleton
public class StreamEventPublisher implements EventListener {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private final DynamicItem<BrokerApi> brokerApiDynamicItem;
  private final StreamEventPublisherConfig config;
  private final Executor executor;
  private final String instanceId;
  private final DynamicItem<BrokerMetrics> brokerMetrics;

  @Inject
  public StreamEventPublisher(
      DynamicItem<BrokerApi> brokerApi,
      StreamEventPublisherConfig config,
      @StreamEventPublisherExecutor Executor executor,
      @Nullable @GerritInstanceId String instanceId,
      DynamicItem<BrokerMetrics> brokerMetrics) {
    this.brokerApiDynamicItem = brokerApi;
    this.config = config;
    this.executor = executor;
    this.instanceId = instanceId;
    this.brokerMetrics = brokerMetrics;
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
        String streamEventTopic = config.getStreamEventsTopic();
        try {
          brokerApi
              .send(streamEventTopic, event)
              .get(config.getPublishingTimeoutInMillis(), TimeUnit.MILLISECONDS);
          brokerMetrics.get().incrementBrokerPublishedMessage();
        } catch (TimeoutException e) {
          log.atSevere().withCause(e).log(
              "Timeout when publishing event '{}' to topic '{}", event, streamEventTopic);
          brokerMetrics.get().incrementBrokerFailedToPublishMessage();
        } catch (Throwable e) {
          log.atSevere().withCause(e).log(
              "Failed to publish event '{}' to topic '{}' - error: {} - stack trace: {}",
              event,
              streamEventTopic,
              e.getMessage(),
              e.getStackTrace());
          brokerMetrics.get().incrementBrokerFailedToPublishMessage();
        }
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
