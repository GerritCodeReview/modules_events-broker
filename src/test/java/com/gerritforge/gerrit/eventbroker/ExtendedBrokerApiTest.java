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
import static com.gerritforge.gerrit.eventbroker.TopicSubscriberWithGroupId.*;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

import com.google.common.eventbus.Subscribe;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExtendedBrokerApiTest {
  private static final int SEND_FUTURE_TIMEOUT = 1;
  private ExtendedBrokerApi brokerApiUnderTest;

  @Before
  public void setup() {
    brokerApiUnderTest = new InProcessBrokerApi();
  }

  @Test
  public void shouldRegisterConsumerPerTopicAndGroupId()
      throws InterruptedException, TimeoutException, ExecutionException {
    Consumer<Event> consumerTopicA = mockEventConsumer();
    ArgumentCaptor<Event> argCaptor = ArgumentCaptor.forClass(Event.class);

    ProjectCreatedEvent eventForTopic = testProjectCreatedEvent("Project name");

    brokerApiUnderTest.receiveAsync("topic", "group-1", consumerTopicA);
    brokerApiUnderTest.send("topic", eventForTopic).get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);

    compareWithExpectedEvent(consumerTopicA, argCaptor, eventForTopic);
  }

  @Test
  public void shouldReturnTopicSubscribersWithGroupId() {
    Consumer<Event> consumerTopicA = mockEventConsumer();
    Consumer<Event> consumerTopicB = mockEventConsumer();
    Consumer<Event> consumerTopicC = mockEventConsumer();

    brokerApiUnderTest.receiveAsync("TopicA", "group-id-1", consumerTopicA);
    brokerApiUnderTest.receiveAsync("TopicB", "group-id-2", consumerTopicB);
    brokerApiUnderTest.receiveAsync("TopicC", consumerTopicC);

    Set<TopicSubscriberWithGroupId> subscribersWithTopicGroupId =
        brokerApiUnderTest.topicSubscribersWithGroupId();
    assertThat(subscribersWithTopicGroupId)
        .containsExactly(
            topicSubscriberWithGroupId("group-id-1", topicSubscriber("TopicA", consumerTopicA)),
            topicSubscriberWithGroupId("group-id-2", topicSubscriber("TopicB", consumerTopicB)));
  }

  @Test
  public void shouldNotRegisterTheSameConsumerTwicePerTopic() {
    Consumer<Event> consumerTopicA = mockEventConsumer();
    brokerApiUnderTest.receiveAsync("topic", "group-id-1", consumerTopicA);
    brokerApiUnderTest.receiveAsync("topic", "group-id-1", consumerTopicA);
    Set<TopicSubscriberWithGroupId> subscribersWithTopicGroupId =
        brokerApiUnderTest.topicSubscribersWithGroupId();
    assertThat(subscribersWithTopicGroupId).hasSize(1);
  }

  @Test
  public void shouldAllConsumersWithDifferentGroupIdConsumeMessage()
      throws InterruptedException, TimeoutException, ExecutionException {
    Consumer<Event> consumerTopicA = mockEventConsumer();
    Consumer<Event> consumerTopicB = mockEventConsumer();
    ArgumentCaptor<Event> argCaptor = ArgumentCaptor.forClass(Event.class);

    ProjectCreatedEvent event = new ProjectCreatedEvent();

    brokerApiUnderTest.receiveAsync("topic", "group-id-1", consumerTopicA);
    brokerApiUnderTest.receiveAsync("topic", consumerTopicB);

    brokerApiUnderTest.send("topic", event).get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);

    compareWithExpectedEvent(consumerTopicA, argCaptor, event);
    compareWithExpectedEvent(consumerTopicB, argCaptor, event);
  }

  /*
   This is due to the fact that InProcessBrokerApi doesn't support
   group id concept
  */
  @Test
  public void shouldConsumeMessageTwicePerTopicAndGroupId()
      throws InterruptedException, TimeoutException, ExecutionException {
    Consumer<Event> consumerTopicA = mockEventConsumer();
    Consumer<Event> consumerTopicB = mockEventConsumer();
    ArgumentCaptor<Event> argCaptor = ArgumentCaptor.forClass(Event.class);

    ProjectCreatedEvent event = new ProjectCreatedEvent();

    brokerApiUnderTest.receiveAsync("topic", "group-id-1", consumerTopicA);
    brokerApiUnderTest.receiveAsync("topic", "group-id-1", consumerTopicB);
    brokerApiUnderTest.send("topic", event).get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);

    compareWithExpectedEvent(consumerTopicA, argCaptor, event);
    compareWithExpectedEvent(consumerTopicB, argCaptor, event);
  }

  @Test
  public void shouldRegisterConsumerWithGroupIdAndWithoutGroupId() {
    Consumer<Event> consumerTopicA = mockEventConsumer();

    brokerApiUnderTest.receiveAsync("topic", "group-id-1", consumerTopicA);
    brokerApiUnderTest.receiveAsync("topic", consumerTopicA);
    Set<TopicSubscriberWithGroupId> subscribersWithTopicGroupId =
        brokerApiUnderTest.topicSubscribersWithGroupId();

    assertThat(subscribersWithTopicGroupId)
        .containsExactly(
            topicSubscriberWithGroupId("group-id-1", topicSubscriber("topic", consumerTopicA)));

    Set<TopicSubscriber> subscribers = brokerApiUnderTest.topicSubscribers();

    assertThat(subscribers).containsExactly(topicSubscriber("topic", consumerTopicA));
  }

  @Test
  public void shouldRegisterMultipleConsumersWithDifferentGroupId() {
    Consumer<Event> consumerA = mockEventConsumer();
    Consumer<Event> consumerB = mockEventConsumer();

    brokerApiUnderTest.receiveAsync("topic", "group-id-1", consumerA);
    brokerApiUnderTest.receiveAsync("topic", "group-id-2", consumerB);
    Set<TopicSubscriberWithGroupId> subscribersWithTopicGroupId =
        brokerApiUnderTest.topicSubscribersWithGroupId();

    assertThat(subscribersWithTopicGroupId)
        .containsExactly(
            topicSubscriberWithGroupId("group-id-1", topicSubscriber("topic", consumerA)),
            topicSubscriberWithGroupId("group-id-2", topicSubscriber("topic", consumerB)));
  }

  private ProjectCreatedEvent testProjectCreatedEvent(String s) {
    ProjectCreatedEvent eventForTopic = new ProjectCreatedEvent();
    eventForTopic.projectName = s;
    return eventForTopic;
  }

  private interface Subscriber extends Consumer<Event> {

    @Override
    @Subscribe
    void accept(Event eventMessage);
  }

  @SuppressWarnings("unchecked")
  private <T> Consumer<T> mockEventConsumer() {
    return (Consumer<T>) Mockito.mock(Subscriber.class);
  }

  private void compareWithExpectedEvent(
      Consumer<Event> eventConsumer, ArgumentCaptor<Event> eventCaptor, Event expectedEvent) {
    verify(eventConsumer, times(1)).accept(eventCaptor.capture());
    assertThat(eventCaptor.getValue()).isEqualTo(expectedEvent);
  }
}
