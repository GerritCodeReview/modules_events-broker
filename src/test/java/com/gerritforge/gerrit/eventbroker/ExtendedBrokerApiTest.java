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

import static com.gerritforge.gerrit.eventbroker.TopicSubscriber.topicSubscriber;
import static com.gerritforge.gerrit.eventbroker.TopicSubscriberWithGroupId.topicSubscriberWithGroupId;
import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.base.Stopwatch;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public abstract class ExtendedBrokerApiTest {
  private static final int SEND_FUTURE_TIMEOUT = 1;
  private ExtendedBrokerApi brokerApiUnderTest;

  protected abstract Duration waitForPollTimeOut();

  protected abstract ExtendedBrokerApi brokerApi();

  @Before
  public void setup() {
    brokerApiUnderTest = brokerApi();
  }

  @Test
  public void shouldConsumerWithGroupIdConsumeMessage()
      throws InterruptedException, TimeoutException, ExecutionException {
    ProjectCreatedEvent event = testProjectCreatedEvent("Project name");
    List<Event> receivedEvents = new ArrayList<>();
    brokerApiUnderTest.receiveAsync("topic", "group-1", receivedEvents::add);
    brokerApiUnderTest.send("topic", event).get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);

    waitUntil(() -> receivedEvents.size() == 1, waitForPollTimeOut());
    assertThat(receivedEvents.get(0).instanceId).isEqualTo(event.instanceId);
  }

  public void shouldAllConsumersWithDifferentGroupIdConsumeMessage()
      throws InterruptedException, TimeoutException, ExecutionException {
    List<Event> receivedEventsGroup1 = new ArrayList<>();
    List<Event> receivedEventsGroup2 = new ArrayList<>();
    ProjectCreatedEvent event = testProjectCreatedEvent("Project name");

    brokerApiUnderTest.receiveAsync("topic", "group-id-1", receivedEventsGroup1::add);
    brokerApiUnderTest.receiveAsync("topic", "group-id-2", receivedEventsGroup2::add);
    brokerApiUnderTest.send("topic", event).get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);

    waitUntil(() -> receivedEventsGroup1.size() == 1, waitForPollTimeOut());
    assertThat(receivedEventsGroup1.get(0).instanceId).isEqualTo(event.instanceId);

    waitUntil(() -> receivedEventsGroup2.size() == 1, waitForPollTimeOut());
    assertThat(receivedEventsGroup2.get(0).instanceId).isEqualTo(event.instanceId);
  }

  @Test
  public void shouldRegisterMultipleConsumersWithDifferentGroupId() {
    List<Event> receivedEventsTopicA = new ArrayList<>();
    Consumer<Event> consumerTopicA = receivedEventsTopicA::add;
    List<Event> receivedEventsTopicB = new ArrayList<>();
    Consumer<Event> consumerTopicB = receivedEventsTopicB::add;
    List<Event> receivedEventsTopicC = new ArrayList<>();

    brokerApiUnderTest.receiveAsync("TopicA", "group-id-1", consumerTopicA);
    brokerApiUnderTest.receiveAsync("TopicB", "group-id-2", consumerTopicB);
    brokerApiUnderTest.receiveAsync("TopicC", receivedEventsTopicC::add);

    Set<TopicSubscriberWithGroupId> subscribersWithTopicGroupId =
        brokerApiUnderTest.topicSubscribersWithGroupId();
    assertThat(subscribersWithTopicGroupId)
        .containsExactly(
            topicSubscriberWithGroupId("group-id-1", topicSubscriber("TopicA", consumerTopicA)),
            topicSubscriberWithGroupId("group-id-2", topicSubscriber("TopicB", consumerTopicB)));
  }

  @Test
  public void shouldRegisterOnceMultipleConsumersForSameTopicAndGroupId() {
    List<Event> receivedEvents = new ArrayList<>();
    Consumer<Event> consumer = receivedEvents::add;

    brokerApiUnderTest.receiveAsync("topic", "group-id-1", consumer);
    brokerApiUnderTest.receiveAsync("topic", "group-id-1", consumer);

    Set<TopicSubscriberWithGroupId> subscribersWithTopicGroupId =
        brokerApiUnderTest.topicSubscribersWithGroupId();
    assertThat(subscribersWithTopicGroupId).hasSize(1);
  }

  private ProjectCreatedEvent testProjectCreatedEvent(String projectName) {
    ProjectCreatedEvent event = new ProjectCreatedEvent();
    event.projectName = projectName;
    return event;
  }

  public static void waitUntil(Supplier<Boolean> waitCondition, Duration timeout)
      throws InterruptedException {
    Stopwatch stopwatch = Stopwatch.createStarted();
    while (!waitCondition.get()) {
      if (stopwatch.elapsed().compareTo(timeout) > 0) {
        throw new InterruptedException();
      }
      MILLISECONDS.sleep(50);
    }
  }
}
