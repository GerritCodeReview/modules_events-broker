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

import com.gerritforge.gerrit.eventbroker.EventsBrokerApiWrapper;
import com.gerritforge.gerrit.eventbroker.metrics.BrokerMetrics;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.TimeUnit;

@Singleton
public class StreamEventPublisher implements EventListener {
  private final StreamEventPublisherConfig config;
  private final DynamicItem<BrokerMetrics> brokerMetrics;
  private final EventsBrokerApiWrapper eventsBrokerApiWrapper;

  @Inject
  public StreamEventPublisher(
      StreamEventPublisherConfig config,
      DynamicItem<BrokerMetrics> brokerMetrics,
      EventsBrokerApiWrapper eventsBrokerApiWrapper) {
    this.config = config;
    this.brokerMetrics = brokerMetrics;
    this.eventsBrokerApiWrapper = eventsBrokerApiWrapper;
  }

  @Override
  public void onEvent(Event event) {
    boolean successfulResult =
        eventsBrokerApiWrapper.sendSyncWithTimeout(
            config.getStreamEventsTopic(),
            event,
            config.getPublishingTimeoutInMillis(),
            TimeUnit.MILLISECONDS);
    if (successfulResult) {
      brokerMetrics.get().incrementBrokerPublishedMessage();
    } else {
      brokerMetrics.get().incrementBrokerFailedToPublishMessage();
    }
  }
}
