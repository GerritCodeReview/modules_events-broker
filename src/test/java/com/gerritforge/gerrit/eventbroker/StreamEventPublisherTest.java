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

import static java.util.concurrent.TimeUnit.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.eventbroker.metrics.BrokerMetrics;
import com.gerritforge.gerrit.eventbroker.publisher.StreamEventPublisher;
import com.gerritforge.gerrit.eventbroker.publisher.StreamEventPublisherConfig;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StreamEventPublisherTest {
  @Mock private DynamicItem<BrokerMetrics> brokerMetricsDynamicItem;
  @Mock private EventsBrokerApiWrapper brokerApi;
  @Mock private BrokerMetrics brokerMetrics;

  private static final String STREAM_EVENTS_TOPIC = "stream-test-topic";
  private static final long PUBLISHING_TIMEOUT = 1000L;
  private static final StreamEventPublisherConfig config =
      new StreamEventPublisherConfig(STREAM_EVENTS_TOPIC, PUBLISHING_TIMEOUT);
  private static final String INSTANCE_ID = "instance-id";

  private StreamEventPublisher objectUnderTest;

  @Before
  public void setup() {
    when(brokerMetricsDynamicItem.get()).thenReturn(brokerMetrics);
    objectUnderTest = new StreamEventPublisher(config, brokerMetricsDynamicItem, brokerApi);
  }

  @Test
  public void shouldAwaitMessagePublishingResultForOnlyAConfigurablePeriodOfTime() {
    Event event = new ProjectCreatedEvent();
    event.instanceId = INSTANCE_ID;

    objectUnderTest.onEvent(event);

    verify(brokerApi)
        .sendSyncWithTimeout(STREAM_EVENTS_TOPIC, event, PUBLISHING_TIMEOUT, MILLISECONDS);
  }

  @Test
  public void shouldIncrementBrokerPublishedMessageMetricOnSuccess() {
    Event event = new ProjectCreatedEvent();
    event.instanceId = INSTANCE_ID;

    when(brokerApi.sendSyncWithTimeout(
            STREAM_EVENTS_TOPIC, event, PUBLISHING_TIMEOUT, MILLISECONDS))
        .thenReturn(true);

    objectUnderTest.onEvent(event);
    verify(brokerMetrics).incrementBrokerPublishedMessage();
  }

  @Test
  public void shouldIncrementBrokerFailedToPublishMessageMetricOnFailure() {
    Event event = new ProjectCreatedEvent();
    event.instanceId = INSTANCE_ID;

    when(brokerApi.sendSyncWithTimeout(
            STREAM_EVENTS_TOPIC, event, PUBLISHING_TIMEOUT, MILLISECONDS))
        .thenReturn(false);

    objectUnderTest.onEvent(event);
    verify(brokerMetrics).incrementBrokerFailedToPublishMessage();
  }
}
