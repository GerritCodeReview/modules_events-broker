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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.eventbroker.log.MessageLogger;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EventsBrokerApiWrapperTest {
  private static final String DEFAULT_INSTANCE_ID = "instance-id";
  @Mock private BrokerApi brokerApi;
  @Mock Event event;
  @Mock MessageLogger msgLog;
  private final String topic = "index";

  private EventsBrokerApiWrapper objectUnderTest;

  @Before
  public void setUp() {
    event.instanceId = DEFAULT_INSTANCE_ID;
    objectUnderTest =
        new EventsBrokerApiWrapper(
            MoreExecutors.directExecutor(),
            DynamicItem.itemOf(BrokerApi.class, brokerApi),
            msgLog,
            DEFAULT_INSTANCE_ID);
  }

  @Test
  public void shouldUpdateMessageLogFileWhenMessagePublished() {
    SettableFuture<Boolean> resultF = SettableFuture.create();
    resultF.set(true);

    when(brokerApi.send(any(), any())).thenReturn(resultF);

    objectUnderTest.send(topic, event);

    verify(msgLog).log(MessageLogger.Direction.PUBLISH, topic, event);
  }

  @Test
  public void shouldNotUpdateMessageLogFileWhenPublishingFails() {
    SettableFuture<Boolean> resultF = SettableFuture.create();
    resultF.setException(new Exception("Force Future failure"));

    when(brokerApi.send(any(), any())).thenReturn(resultF);

    objectUnderTest.send(topic, event);

    verify(msgLog, never()).log(MessageLogger.Direction.PUBLISH, topic, event);
  }

  @Test
  public void shouldProvideThePublishingResultWhenTheClientWaitsForPublishingToFinish() {
    SettableFuture<Boolean> resultF = SettableFuture.create();
    resultF.set(true);

    when(brokerApi.send(any(), any())).thenReturn(resultF);

    boolean result = objectUnderTest.sendSync(topic, event);

    assertThat(result).isTrue();
  }

  @Test
  public void
      shouldProvideThePublishingResultWhenTheClientWaitsForPublishingToFinishWithinATimePeriod() {
    SettableFuture<Boolean> resultF = SettableFuture.create();
    resultF.set(true);

    when(brokerApi.send(any(), any())).thenReturn(resultF);

    boolean result = objectUnderTest.sendSyncWithTimeout(topic, event, 100, TimeUnit.MILLISECONDS);

    assertThat(result).isTrue();
  }

  @Test
  public void
      shouldIndicateFailureWhenMessagePublishingFailedAndTheClientWaitsForThePublishingToFinish() {
    SettableFuture<Boolean> resultF = SettableFuture.create();
    resultF.setException(new Exception("Force Future failure"));

    when(brokerApi.send(any(), any())).thenReturn(resultF);

    try {
      objectUnderTest.send(topic, event).get();
    } catch (Exception e) {
      // expected
    }

    verify(msgLog, never()).log(MessageLogger.Direction.PUBLISH, topic, event);
  }

  @Test
  public void shouldIndicateFailureWhenMessagePublishingTimesOut() {
    ListeningExecutorService executor =
        MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());

    ListenableFuture<Boolean> messagePublishingF =
        executor.submit(
            () -> {
              try {
                Thread.sleep(100);
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
              return true;
            });

    when(brokerApi.send(any(), any())).thenReturn(messagePublishingF);

    boolean result = objectUnderTest.sendSyncWithTimeout(topic, event, 10, TimeUnit.MILLISECONDS);

    assertThat(result).isFalse();
  }

  @Test
  public void shouldSkipEventWithoutInstanceId() {
    ProjectCreatedEvent event = new ProjectCreatedEvent();
    event.instanceId = null;

    objectUnderTest.send(topic, event);

    verify(brokerApi, never()).send(any(), eq(event));
  }

  @Test
  public void shouldSkipEventWithEmptyInstanceId() {
    ProjectCreatedEvent event = new ProjectCreatedEvent();
    event.instanceId = "";

    objectUnderTest.send(topic, event);

    verify(brokerApi, never()).send(any(), eq(event));
  }

  @Test
  public void shouldSkipForwardedEvent() {
    ProjectCreatedEvent event = new ProjectCreatedEvent();
    event.instanceId = "different-node-instance-id";

    objectUnderTest.send(topic, event);

    verify(brokerApi, never()).send(topic, event);
  }
}
