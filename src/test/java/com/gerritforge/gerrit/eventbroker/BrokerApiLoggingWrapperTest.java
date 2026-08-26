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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.eventbroker.log.MessageLogger;
import com.google.common.util.concurrent.Futures;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BrokerApiLoggingWrapperTest {
  private static final String TOPIC = "index";

  @Mock private BrokerApi brokerApi;
  @Mock private MessageLogger msgLog;

  private final Event event = new ProjectCreatedEvent();

  private BrokerApiLoggingWrapper objectUnderTest;

  @Before
  public void setUp() {
    objectUnderTest = new BrokerApiLoggingWrapper(DynamicItem.itemOf(BrokerApi.class, brokerApi), msgLog);
  }

  @Test
  public void shouldLogPublishedMessage() {
    brokerReturns(true);

    objectUnderTest.send(TOPIC, event);

    verify(brokerApi).send(TOPIC, event);
    verify(msgLog).log(MessageLogger.Direction.PUBLISH, TOPIC, event);
  }

  @Test
  public void shouldLogMessageWithTheRequestedDirection() {
    brokerReturns(true);

    objectUnderTest.send(TOPIC, event, MessageLogger.Direction.REQUEUE);

    verify(brokerApi).send(TOPIC, event);
    verify(msgLog).log(MessageLogger.Direction.REQUEUE, TOPIC, event);
  }

  @Test
  public void shouldNotLogWhenPublishingReturnsFalse() {
    brokerReturns(false);

    objectUnderTest.send(TOPIC, event);

    verify(msgLog, never()).log(any(), any(), any());
  }

  @Test
  public void shouldNotLogWhenPublishingFails() {
    when(brokerApi.send(any(), any()))
        .thenReturn(Futures.immediateFailedFuture(new Exception("Force future failure")));

    objectUnderTest.send(TOPIC, event);

    verify(msgLog, never()).log(any(), any(), any());
  }

  private void brokerReturns(boolean result) {
    when(brokerApi.send(any(), any())).thenReturn(Futures.immediateFuture(result));
  }
}
