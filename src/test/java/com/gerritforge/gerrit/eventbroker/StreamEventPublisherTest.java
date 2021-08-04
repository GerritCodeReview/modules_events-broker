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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
  @Mock private DynamicItem<BrokerApi> brokerApiDynamicItem;
  @Mock private BrokerApi brokerApi;
  private String streamEventsTopic = "stream-test-topic";
  private String instanceId = "instance-id";

  private StreamEventPublisher objectUnderTest;

  @Before
  public void setup() {
    when(brokerApiDynamicItem.get()).thenReturn(brokerApi);
    objectUnderTest = new StreamEventPublisher(brokerApiDynamicItem, streamEventsTopic, instanceId);
  }

  @Test
  public void shouldSendLocallyGeneratedEvent() {
    Event event = new ProjectCreatedEvent();
    event.instanceId = instanceId;

    objectUnderTest.onEvent(event);
    verify(brokerApi, times(1)).send(streamEventsTopic, event);
  }

  @Test
  public void shouldSendEventWhenNodeInstanceIdIsNull() {
    Event event = new ProjectCreatedEvent();
    event.instanceId = instanceId;

    objectUnderTest = new StreamEventPublisher(brokerApiDynamicItem, streamEventsTopic, null);
    objectUnderTest.onEvent(event);
    verify(brokerApi, times(1)).send(streamEventsTopic, event);
  }

  @Test
  public void shouldSkipForwardedEvent() {
    Event event = new ProjectCreatedEvent();
    event.instanceId = "different-node-instance-id";

    objectUnderTest.onEvent(event);
    verify(brokerApi, never()).send(streamEventsTopic, event);
  }

  @Test
  public void shouldSkipEventWithoutInstanceId() {
    Event event = new ProjectCreatedEvent();
    event.instanceId = null;

    objectUnderTest.onEvent(event);
    verify(brokerApi, never()).send(streamEventsTopic, event);
  }
}
