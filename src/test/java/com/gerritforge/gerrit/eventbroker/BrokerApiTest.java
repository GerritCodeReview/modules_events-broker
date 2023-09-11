// Copyright (C) 2019 The Android Open Source Project
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
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.eventbus.Subscribe;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
abstract class BrokerApiTest {

  public static final int SEND_FUTURE_TIMEOUT = 1;
  @Captor ArgumentCaptor<Event> eventCaptor;
  Consumer<Event> eventConsumer;

  BrokerApi brokerApiUnderTest;
  UUID instanceId = UUID.randomUUID();
  private Gson gson = new Gson();

  protected abstract BrokerApi brokerApi();

  @Before
  public void setup() {
    brokerApiUnderTest = brokerApi();
    eventConsumer = mockEventConsumer();
  }

  @Test
  public void shouldSendEvent() throws InterruptedException, TimeoutException, ExecutionException {
    ProjectCreatedEvent event = new ProjectCreatedEvent();

    brokerApiUnderTest.receiveAsync("topic", eventConsumer);

    assertThat(brokerApiUnderTest.send("topic", wrap(event)).get(1, TimeUnit.SECONDS)).isTrue();

    compareWithExpectedEvent(eventConsumer, eventCaptor, event);
  }

  private Event wrap(ProjectCreatedEvent event) {
    return event;
  }

  @Test
  public void shouldRegisterConsumerPerTopic()
      throws InterruptedException, TimeoutException, ExecutionException {
    Consumer<Event> secondConsumer = mockEventConsumer();
    ArgumentCaptor<Event> secondArgCaptor = ArgumentCaptor.forClass(Event.class);

    ProjectCreatedEvent eventForTopic = testProjectCreatedEvent("Project name");
    ProjectCreatedEvent eventForTopic2 = testProjectCreatedEvent("Project name 2");

    brokerApiUnderTest.receiveAsync("topic", eventConsumer);
    brokerApiUnderTest.receiveAsync("topic2", secondConsumer);
    brokerApiUnderTest
        .send("topic", wrap(eventForTopic))
        .get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);
    brokerApiUnderTest
        .send("topic2", wrap(eventForTopic2))
        .get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);

    compareWithExpectedEvent(eventConsumer, eventCaptor, eventForTopic);
    compareWithExpectedEvent(secondConsumer, secondArgCaptor, eventForTopic2);
  }

  @Test
  public void shouldReturnMapOfConsumersPerTopic() {
    Consumer<Event> firstConsumerTopicA = mockEventConsumer();

    Consumer<Event> secondConsumerTopicA = mockEventConsumer();
    Consumer<Event> thirdConsumerTopicB = mockEventConsumer();

    brokerApiUnderTest.receiveAsync("TopicA", firstConsumerTopicA);
    brokerApiUnderTest.receiveAsync("TopicA", secondConsumerTopicA);
    brokerApiUnderTest.receiveAsync("TopicB", thirdConsumerTopicB);

    Set<TopicSubscriber> consumersMap = brokerApiUnderTest.topicSubscribers();

    assertThat(consumersMap).isNotNull();
    assertThat(consumersMap).isNotEmpty();
    assertThat(consumersMap)
        .containsExactly(
            topicSubscriber("TopicA", firstConsumerTopicA),
            topicSubscriber("TopicA", secondConsumerTopicA),
            topicSubscriber("TopicB", thirdConsumerTopicB));
  }

  @Test
  public void shouldDeliverAsynchronouslyEventToAllRegisteredConsumers()
      throws InterruptedException, TimeoutException, ExecutionException {
    Consumer<Event> secondConsumer = mockEventConsumer();
    ArgumentCaptor<Event> secondArgCaptor = ArgumentCaptor.forClass(Event.class);

    ProjectCreatedEvent event = testProjectCreatedEvent("Project name");

    brokerApiUnderTest.receiveAsync("topic", eventConsumer);
    brokerApiUnderTest.receiveAsync("topic", secondConsumer);
    brokerApiUnderTest.send("topic", wrap(event)).get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);

    compareWithExpectedEvent(eventConsumer, eventCaptor, event);
    compareWithExpectedEvent(secondConsumer, secondArgCaptor, event);
  }

  @Test
  public void shouldReceiveEventsOnlyFromRegisteredTopic()
      throws InterruptedException, TimeoutException, ExecutionException {

    ProjectCreatedEvent eventForTopic = testProjectCreatedEvent("Project name");

    ProjectCreatedEvent eventForTopic2 = testProjectCreatedEvent("Project name 2");

    brokerApiUnderTest.receiveAsync("topic", eventConsumer);
    brokerApiUnderTest
        .send("topic", wrap(eventForTopic))
        .get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);
    brokerApiUnderTest
        .send("topic2", wrap(eventForTopic2))
        .get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);

    compareWithExpectedEvent(eventConsumer, eventCaptor, eventForTopic);
  }

  @Test
  public void shouldNotRegisterTheSameConsumerTwicePerTopic()
      throws InterruptedException, TimeoutException, ExecutionException {
    ProjectCreatedEvent event = new ProjectCreatedEvent();

    brokerApiUnderTest.receiveAsync("topic", eventConsumer);
    brokerApiUnderTest.receiveAsync("topic", eventConsumer);
    brokerApiUnderTest.send("topic", wrap(event)).get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);

    compareWithExpectedEvent(eventConsumer, eventCaptor, event);
  }

  @Test
  public void shouldReconnectSubscribers()
      throws InterruptedException, TimeoutException, ExecutionException {
    ArgumentCaptor<Event> newConsumerArgCaptor = ArgumentCaptor.forClass(Event.class);

    ProjectCreatedEvent eventForTopic = testProjectCreatedEvent("Project name");

    brokerApiUnderTest.receiveAsync("topic", eventConsumer);
    brokerApiUnderTest
        .send("topic", wrap(eventForTopic))
        .get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);

    compareWithExpectedEvent(eventConsumer, eventCaptor, eventForTopic);

    Consumer<Event> newConsumer = mockEventConsumer();

    clearInvocations(eventConsumer);

    brokerApiUnderTest.disconnect();
    brokerApiUnderTest.receiveAsync("topic", newConsumer);
    brokerApiUnderTest
        .send("topic", wrap(eventForTopic))
        .get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);

    compareWithExpectedEvent(newConsumer, newConsumerArgCaptor, eventForTopic);
    verify(eventConsumer, never()).accept(eventCaptor.capture());
  }

  @Test
  public void shouldDisconnectSubscribers()
      throws InterruptedException, TimeoutException, ExecutionException {
    ProjectCreatedEvent eventForTopic = testProjectCreatedEvent("Project name");

    brokerApiUnderTest.receiveAsync("topic", eventConsumer);
    brokerApiUnderTest.disconnect();

    brokerApiUnderTest
        .send("topic", wrap(eventForTopic))
        .get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);

    verify(eventConsumer, never()).accept(eventCaptor.capture());
  }

  @Test
  public void shouldBeAbleToSwitchBrokerAndReconnectSubscribers()
      throws InterruptedException, TimeoutException, ExecutionException {
    ArgumentCaptor<Event> newConsumerArgCaptor = ArgumentCaptor.forClass(Event.class);

    ProjectCreatedEvent eventForTopic = testProjectCreatedEvent("Project name");

    BrokerApi secondaryBroker = brokerApi();
    brokerApiUnderTest.disconnect();
    secondaryBroker.receiveAsync("topic", eventConsumer);

    clearInvocations(eventConsumer);

    brokerApiUnderTest
        .send("topic", wrap(eventForTopic))
        .get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);
    verify(eventConsumer, never()).accept(eventCaptor.capture());

    clearInvocations(eventConsumer);
    secondaryBroker.send("topic", wrap(eventForTopic)).get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);

    compareWithExpectedEvent(eventConsumer, newConsumerArgCaptor, eventForTopic);
  }

  @Test
  public void shouldReplayAllEvents()
      throws InterruptedException, TimeoutException, ExecutionException {
    ProjectCreatedEvent event = new ProjectCreatedEvent();

    brokerApiUnderTest.receiveAsync("topic", eventConsumer);

    assertThat(
            brokerApiUnderTest
                .send("topic", wrap(event))
                .get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS))
        .isTrue();

    verify(eventConsumer, times(1)).accept(eventCaptor.capture());
    compareWithExpectedEvent(eventConsumer, eventCaptor, event);
    reset(eventConsumer);

    brokerApiUnderTest.replayAllEvents("topic");
    verify(eventConsumer, times(1)).accept(eventCaptor.capture());
    compareWithExpectedEvent(eventConsumer, eventCaptor, event);
  }

  @Test
  public void shouldSkipReplayAllEventsWhenTopicDoesNotExists() {
    brokerApiUnderTest.replayAllEvents("unexistentTopic");
    verify(eventConsumer, times(0)).accept(eventCaptor.capture());
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
  protected <T> Consumer<T> mockEventConsumer() {
    return (Consumer<T>) Mockito.mock(Subscriber.class);
  }

  protected void compareWithExpectedEvent(
      Consumer<Event> eventConsumer, ArgumentCaptor<Event> eventCaptor, Event expectedEvent) {
    verify(eventConsumer, times(1)).accept(eventCaptor.capture());
    assertThat(eventCaptor.getValue()).isEqualTo(expectedEvent);
  }

  private JsonObject eventToJson(Event event) {
    return gson.toJsonTree(event).getAsJsonObject();
  }
}
