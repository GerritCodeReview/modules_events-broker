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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BrokerApiTest {

  @Captor ArgumentCaptor<EventMessage> eventCaptor;
  Consumer<EventMessage> eventConsumer;

  BrokerApi brokerApiUnderTest;
  UUID instanceId = UUID.randomUUID();
  private Gson gson = new Gson();
  ExecutorService executorService =
      MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));

  @Before
  public void setup() {
    brokerApiUnderTest = new InProcessBrokerApi();
    eventConsumer = mockEventConsumer();
  }

  @Test
  public void shouldSendEvent() throws InterruptedException {
    ProjectCreatedEvent event = new ProjectCreatedEvent();

    brokerApiUnderTest.receiveAsync("topic", eventConsumer);

    ListenableFuture<Boolean> resultF = brokerApiUnderTest.send("topic", wrap(event));
    waitForMessageToBeSent(resultF);

    compareWithExpectedEvent(eventConsumer, eventCaptor, event);
  }

  private EventMessage wrap(ProjectCreatedEvent event) {
    return brokerApiUnderTest.newMessage(instanceId, event);
  }

  @Test
  public void shouldRegisterConsumerPerTopic() {
    Consumer<EventMessage> secondConsumer = mockEventConsumer();
    ArgumentCaptor<EventMessage> secondArgCaptor = ArgumentCaptor.forClass(EventMessage.class);

    ProjectCreatedEvent eventForTopic = testProjectCreatedEvent("Project name");
    ProjectCreatedEvent eventForTopic2 = testProjectCreatedEvent("Project name 2");

    brokerApiUnderTest.receiveAsync("topic", eventConsumer);
    brokerApiUnderTest.receiveAsync("topic2", secondConsumer);
    ListenableFuture<Boolean> resultF1 = brokerApiUnderTest.send("topic", wrap(eventForTopic));
    ListenableFuture<Boolean> resultF2 = brokerApiUnderTest.send("topic2", wrap(eventForTopic2));
    waitForMessageToBeSent(resultF1);
    waitForMessageToBeSent(resultF2);

    compareWithExpectedEvent(eventConsumer, eventCaptor, eventForTopic);
    compareWithExpectedEvent(secondConsumer, secondArgCaptor, eventForTopic2);
  }

  @Test
  public void shouldReturnMapOfConsumersPerTopic() {
    Consumer<EventMessage> firstConsumerTopicA = mockEventConsumer();

    Consumer<EventMessage> secondConsumerTopicA = mockEventConsumer();
    Consumer<EventMessage> thirdConsumerTopicB = mockEventConsumer();

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
      throws InterruptedException {
    Consumer<EventMessage> secondConsumer = mockEventConsumer();
    ArgumentCaptor<EventMessage> secondArgCaptor = ArgumentCaptor.forClass(EventMessage.class);

    ProjectCreatedEvent event = testProjectCreatedEvent("Project name");

    brokerApiUnderTest.receiveAsync("topic", eventConsumer);
    brokerApiUnderTest.receiveAsync("topic", secondConsumer);
    ListenableFuture<Boolean> resultF = brokerApiUnderTest.send("topic", wrap(event));
    waitForMessageToBeSent(resultF);

    compareWithExpectedEvent(eventConsumer, eventCaptor, event);
    compareWithExpectedEvent(secondConsumer, secondArgCaptor, event);
  }

  @Test
  public void shouldReceiveEventsOnlyFromRegisteredTopic() {

    ProjectCreatedEvent eventForTopic = testProjectCreatedEvent("Project name");

    ProjectCreatedEvent eventForTopic2 = testProjectCreatedEvent("Project name 2");

    brokerApiUnderTest.receiveAsync("topic", eventConsumer);
    ListenableFuture<Boolean> resultF1 = brokerApiUnderTest.send("topic", wrap(eventForTopic));
    ListenableFuture<Boolean> resultF2 = brokerApiUnderTest.send("topic2", wrap(eventForTopic2));
    waitForMessageToBeSent(resultF1);
    waitForMessageToBeSent(resultF2);

    compareWithExpectedEvent(eventConsumer, eventCaptor, eventForTopic);
  }

  @Test
  public void shouldNotRegisterTheSameConsumerTwicePerTopic() {
    ProjectCreatedEvent event = new ProjectCreatedEvent();

    brokerApiUnderTest.receiveAsync("topic", eventConsumer);
    brokerApiUnderTest.receiveAsync("topic", eventConsumer);
    ListenableFuture<Boolean> resultF = brokerApiUnderTest.send("topic", wrap(event));
    waitForMessageToBeSent(resultF);

    compareWithExpectedEvent(eventConsumer, eventCaptor, event);
  }

  @Test
  public void shouldReconnectSubscribers() {
    ArgumentCaptor<EventMessage> newConsumerArgCaptor = ArgumentCaptor.forClass(EventMessage.class);

    ProjectCreatedEvent eventForTopic = testProjectCreatedEvent("Project name");

    brokerApiUnderTest.receiveAsync("topic", eventConsumer);
    waitForMessageToBeSent(brokerApiUnderTest.send("topic", wrap(eventForTopic)));

    compareWithExpectedEvent(eventConsumer, eventCaptor, eventForTopic);

    Consumer<EventMessage> newConsumer = mockEventConsumer();

    clearInvocations(eventConsumer);

    brokerApiUnderTest.disconnect();
    brokerApiUnderTest.receiveAsync("topic", newConsumer);
    waitForMessageToBeSent(brokerApiUnderTest.send("topic", wrap(eventForTopic)));

    compareWithExpectedEvent(newConsumer, newConsumerArgCaptor, eventForTopic);
    verify(eventConsumer, never()).accept(eventCaptor.capture());
  }

  @Test
  public void shouldDisconnectSubscribers() {
    ProjectCreatedEvent eventForTopic = testProjectCreatedEvent("Project name");

    brokerApiUnderTest.receiveAsync("topic", eventConsumer);
    brokerApiUnderTest.disconnect();

    waitForMessageToBeSent(brokerApiUnderTest.send("topic", wrap(eventForTopic)));

    verify(eventConsumer, never()).accept(eventCaptor.capture());
  }

  @Test
  public void shouldBeAbleToSwitchBrokerAndReconnectSubscribers() {
    ArgumentCaptor<EventMessage> newConsumerArgCaptor = ArgumentCaptor.forClass(EventMessage.class);

    ProjectCreatedEvent eventForTopic = testProjectCreatedEvent("Project name");

    BrokerApi secondaryBroker = new InProcessBrokerApi();
    brokerApiUnderTest.disconnect();
    secondaryBroker.receiveAsync("topic", eventConsumer);

    clearInvocations(eventConsumer);

    waitForMessageToBeSent(brokerApiUnderTest.send("topic", wrap(eventForTopic)));
    verify(eventConsumer, never()).accept(eventCaptor.capture());

    clearInvocations(eventConsumer);
    waitForMessageToBeSent(secondaryBroker.send("topic", wrap(eventForTopic)));

    compareWithExpectedEvent(eventConsumer, newConsumerArgCaptor, eventForTopic);
  }

  @Test
  public void shouldReplayAllEvents() {
    ProjectCreatedEvent event = new ProjectCreatedEvent();

    brokerApiUnderTest.receiveAsync("topic", eventConsumer);

    waitForMessageToBeSent(brokerApiUnderTest.send("topic", wrap(event)));

    verify(eventConsumer, times(1)).accept(eventCaptor.capture());
    compareWithExpectedEvent(eventConsumer, eventCaptor, event);
    reset(eventConsumer);

    brokerApiUnderTest.replayAllEvents("topic");
    verify(eventConsumer, timeout(2000).times(1)).accept(eventCaptor.capture());
    compareWithExpectedEvent(eventConsumer, eventCaptor, event);
  }

  @Test
  public void shouldSkipReplayAllEventsWhenTopicDoesNotExists() {
    brokerApiUnderTest.replayAllEvents("unexistentTopic");
    verify(eventConsumer, times(0)).accept(eventCaptor.capture());
  }

  private void waitForMessageToBeSent(ListenableFuture<Boolean> resultF) {
    try {
      CountDownLatch lock = new CountDownLatch(1);
      resultF.addListener(
          new Runnable() {
            public void run() {
              lock.countDown();
            }
          },
          executorService);
      lock.await(2, TimeUnit.SECONDS);
    } catch (InterruptedException ie) {
      fail("Caught exception while waiting for message to be sent " + ie);
    }
  }

  private ProjectCreatedEvent testProjectCreatedEvent(String s) {
    ProjectCreatedEvent eventForTopic = new ProjectCreatedEvent();
    eventForTopic.projectName = s;
    return eventForTopic;
  }

  private interface Subscriber extends Consumer<EventMessage> {

    @Override
    @Subscribe
    void accept(EventMessage eventMessage);
  }

  @SuppressWarnings("unchecked")
  private <T> Consumer<T> mockEventConsumer() {
    return (Consumer<T>) Mockito.mock(Subscriber.class);
  }

  private void compareWithExpectedEvent(
      Consumer<EventMessage> eventConsumer,
      ArgumentCaptor<EventMessage> eventCaptor,
      Event expectedEvent) {
    verify(eventConsumer, times(1)).accept(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getEvent()).isEqualTo(expectedEvent);
  }

  private JsonObject eventToJson(Event event) {
    return gson.toJsonTree(event).getAsJsonObject();
  }
}
