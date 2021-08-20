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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StreamEventPublisherTest {
  @Mock private DynamicItem<BrokerApi> brokerApiDynamicItem;
  @Mock private BrokerApi brokerApi;
  private static final String STREAM_EVENTS_TOPIC = "stream-test-topic";
  private static final String INSTANCE_ID = "instance-id";
  private static final Executor EXECUTOR = MoreExecutors.directExecutor();

  private StreamEventPublisher objectUnderTest;

  @Before
  public void setup() {
    when(brokerApiDynamicItem.get()).thenReturn(brokerApi);
    when(brokerApi.send(any(), any())).thenReturn(Futures.immediateFuture(true));
    objectUnderTest =
        new StreamEventPublisher(brokerApiDynamicItem, STREAM_EVENTS_TOPIC, EXECUTOR, INSTANCE_ID);
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
        new StreamEventPublisher(brokerApiDynamicItem, STREAM_EVENTS_TOPIC, EXECUTOR, null);
    objectUnderTest.onEvent(event);
    verify(brokerApi, times(1)).send(STREAM_EVENTS_TOPIC, event);
  }

  @Test
  public void shouldSkipEventWhenNodeInstanceIdIsNull() {
    Event event = new ProjectCreatedEvent();
    event.instanceId = INSTANCE_ID;

    objectUnderTest =
        new StreamEventPublisher(brokerApiDynamicItem, STREAM_EVENTS_TOPIC, EXECUTOR, null);
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
}
