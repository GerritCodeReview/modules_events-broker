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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.eventbroker.metrics.BrokerMetrics;
import com.gerritforge.gerrit.eventbroker.publisher.StreamEventPublisher;
import com.gerritforge.gerrit.eventbroker.publisher.StreamEventPublisherConfig;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StreamEventPublisherTest {
  @Mock private DynamicItem<BrokerApi> brokerApiDynamicItem;
  @Mock private DynamicItem<BrokerMetrics> brokerMetricsDynamicItem;
  @Mock private BrokerApi brokerApi;
  @Mock private BrokerMetrics brokerMetrics;

  private static final String STREAM_EVENTS_TOPIC = "stream-test-topic";
  private static final long PUBLISHING_TIMEOUT = 1000L;
  private static final StreamEventPublisherConfig config =
      new StreamEventPublisherConfig(STREAM_EVENTS_TOPIC, PUBLISHING_TIMEOUT);
  private static final String INSTANCE_ID = "instance-id";
  private static final Executor EXECUTOR = MoreExecutors.directExecutor();

  private StreamEventPublisher objectUnderTest;

  @Before
  public void setup() {
    when(brokerApiDynamicItem.get()).thenReturn(brokerApi);
    when(brokerMetricsDynamicItem.get()).thenReturn(brokerMetrics);
    when(brokerApi.send(any(), any())).thenReturn(Futures.immediateFuture(true));
    objectUnderTest =
        new StreamEventPublisher(
            brokerApiDynamicItem, config, EXECUTOR, INSTANCE_ID, brokerMetricsDynamicItem);
  }

  @Test
  public void shouldSendLocallyGeneratedEvent() {
    Event event = new ProjectCreatedEvent();
    event.instanceId = INSTANCE_ID;

    objectUnderTest.onEvent(event);
    verify(brokerApi, times(1)).send(STREAM_EVENTS_TOPIC, event);
  }

  @Test
  public void shouldSendEventWhenNodeInstanceIdIsNullAndEventInstanceUdIsNull() {
    Event event = new ProjectCreatedEvent();
    event.instanceId = null;

    objectUnderTest =
        new StreamEventPublisher(
            brokerApiDynamicItem, config, EXECUTOR, null, brokerMetricsDynamicItem);
    objectUnderTest.onEvent(event);
    verify(brokerApi, times(1)).send(STREAM_EVENTS_TOPIC, event);
  }

  @Test
  public void shouldSkipEventWhenNodeInstanceIdIsNull() {
    Event event = new ProjectCreatedEvent();
    event.instanceId = INSTANCE_ID;

    objectUnderTest =
        new StreamEventPublisher(
            brokerApiDynamicItem, config, EXECUTOR, null, brokerMetricsDynamicItem);
    objectUnderTest.onEvent(event);
    verify(brokerApi, never()).send(STREAM_EVENTS_TOPIC, event);
  }

  @Test
  public void shouldSkipForwardedEvent() {
    Event event = new ProjectCreatedEvent();
    event.instanceId = "different-node-instance-id";

    objectUnderTest.onEvent(event);
    verify(brokerApi, never()).send(STREAM_EVENTS_TOPIC, event);
  }

  @Test
  public void shouldSkipEventWithoutInstanceId() {
    Event event = new ProjectCreatedEvent();
    event.instanceId = null;

    objectUnderTest.onEvent(event);
    verify(brokerApi, never()).send(STREAM_EVENTS_TOPIC, event);
  }

  @Test
  public void shouldIncrementBrokerPublishedMessageMetric() {
    Event event = new ProjectCreatedEvent();
    event.instanceId = INSTANCE_ID;

    objectUnderTest.onEvent(event);
    verify(brokerMetrics, times(1)).incrementBrokerPublishedMessage();
  }

  @Test
  public void shouldIncrementBrokerFailedToPublishMessageMetricWhenTimeoutException()
      throws InterruptedException, ExecutionException, TimeoutException {
    Event event = new ProjectCreatedEvent();
    event.instanceId = INSTANCE_ID;
    when(brokerApi.send(any(), any()))
        .thenReturn(Futures.immediateFailedFuture(new TimeoutException()));

    objectUnderTest.onEvent(event);
    verify(brokerMetrics, times(1)).incrementBrokerFailedToPublishMessage();
  }

  @Test
  public void shouldIncrementBrokerFailedToPublishMessageMetricWhenException()
      throws InterruptedException, ExecutionException, TimeoutException {
    Event event = new ProjectCreatedEvent();
    event.instanceId = INSTANCE_ID;
    when(brokerApi.send(any(), any()))
        .thenReturn(
            Futures.immediateFailedFuture(new Exception("Exception during message publishing")));

    objectUnderTest.onEvent(event);
    verify(brokerMetrics, times(1)).incrementBrokerFailedToPublishMessage();
  }
}
