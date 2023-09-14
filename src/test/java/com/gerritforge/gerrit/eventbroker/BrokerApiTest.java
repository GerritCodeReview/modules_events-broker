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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.base.Stopwatch;
import com.google.common.eventbus.Subscribe;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import com.google.gson.Gson;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@Ignore
@RunWith(MockitoJUnitRunner.class)
public abstract class BrokerApiTest {
  private static final Duration WAIT_PATIENCE = Duration.ofSeconds(5L);

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

  @Rule public TestName testName = new TestName();

  @Test
  public void shouldSendEvent() throws InterruptedException, TimeoutException, ExecutionException {
    ProjectCreatedEvent event = new ProjectCreatedEvent();

    brokerApiUnderTest.receiveAsync(testTopic(), eventConsumer);

    assertThat(brokerApiUnderTest.send(testTopic(), wrap(event)).get(1, TimeUnit.SECONDS)).isTrue();

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

    brokerApiUnderTest.receiveAsync(testTopic(), eventConsumer);
    brokerApiUnderTest.receiveAsync(testTopic() + "_2", secondConsumer);
    brokerApiUnderTest
        .send(testTopic(), wrap(eventForTopic))
        .get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);
    brokerApiUnderTest
        .send(testTopic() + "_2", wrap(eventForTopic2))
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
  public void shouldReceiveEventsOnlyFromRegisteredTopic()
      throws InterruptedException, TimeoutException, ExecutionException {

    ProjectCreatedEvent eventForTopic = testProjectCreatedEvent("Project name");

    ProjectCreatedEvent eventForTopic2 = testProjectCreatedEvent("Project name 2");

    brokerApiUnderTest.receiveAsync(testTopic(), eventConsumer);
    brokerApiUnderTest
        .send(testTopic(), wrap(eventForTopic))
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

    brokerApiUnderTest.receiveAsync(testTopic(), eventConsumer);
    brokerApiUnderTest.receiveAsync(testTopic(), eventConsumer);
    brokerApiUnderTest.send(testTopic(), wrap(event)).get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);

    compareWithExpectedEvent(eventConsumer, eventCaptor, event);
  }

  @Test
  public void shouldReconnectSubscribers()
      throws InterruptedException, TimeoutException, ExecutionException {
    ArgumentCaptor<Event> newConsumerArgCaptor = ArgumentCaptor.forClass(Event.class);

    ProjectCreatedEvent eventForTopic = testProjectCreatedEvent("Project name");

    brokerApiUnderTest.receiveAsync(testTopic(), eventConsumer);
    brokerApiUnderTest
        .send(testTopic(), wrap(eventForTopic))
        .get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);

    compareWithExpectedEvent(eventConsumer, eventCaptor, eventForTopic);

    Consumer<Event> newConsumer = mockEventConsumer();

    clearInvocations(eventConsumer);

    brokerApiUnderTest.disconnect();
    brokerApiUnderTest.receiveAsync(testTopic(), newConsumer);
    brokerApiUnderTest
        .send(testTopic(), wrap(eventForTopic))
        .get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);

    compareWithExpectedEvent(newConsumer, newConsumerArgCaptor, eventForTopic);
    verify(eventConsumer, never()).accept(eventCaptor.capture());
  }

  @Test
  public void shouldDisconnectSubscribers()
      throws InterruptedException, TimeoutException, ExecutionException {
    ProjectCreatedEvent eventForTopic = testProjectCreatedEvent("Project name");

    brokerApiUnderTest.receiveAsync(testTopic(), eventConsumer);
    brokerApiUnderTest.disconnect();

    brokerApiUnderTest
        .send(testTopic(), wrap(eventForTopic))
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
    secondaryBroker.receiveAsync(testTopic(), eventConsumer);

    clearInvocations(eventConsumer);

    brokerApiUnderTest
        .send(testTopic(), wrap(eventForTopic))
        .get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);
    waitUntilUnchecked(
        () -> {
          verify(eventConsumer, never()).accept(eventCaptor.capture());
          return true;
        });

    clearInvocations(eventConsumer);
    secondaryBroker
        .send(testTopic(), wrap(eventForTopic))
        .get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS);

    compareWithExpectedEvent(eventConsumer, newConsumerArgCaptor, eventForTopic);
  }

  @Test
  public void shouldReplayAllEvents()
      throws InterruptedException, TimeoutException, ExecutionException {
    ProjectCreatedEvent event = new ProjectCreatedEvent();

    brokerApiUnderTest.receiveAsync(testTopic(), eventConsumer);

    assertThat(
            brokerApiUnderTest
                .send(testTopic(), wrap(event))
                .get(SEND_FUTURE_TIMEOUT, TimeUnit.SECONDS))
        .isTrue();

    waitUntilUnchecked(
        uncheckedResultOf(
            () -> {
              verify(eventConsumer, times(1)).accept(eventCaptor.capture());
              return true;
            }));

    compareWithExpectedEvent(eventConsumer, eventCaptor, event);
    reset(eventConsumer);

    brokerApiUnderTest.replayAllEvents(testTopic());

    waitUntilUnchecked(
        () -> {
          verify(eventConsumer, times(1)).accept(eventCaptor.capture());
          return true;
        });
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
  private <T> Consumer<T> mockEventConsumer() {
    return (Consumer<T>) Mockito.mock(Subscriber.class);
  }

  protected void compareWithExpectedEvent(
      Consumer<Event> eventConsumer, ArgumentCaptor<Event> eventCaptor, Event expectedEvent)
      throws InterruptedException {
    waitUntilUnchecked(
        () -> {
          verify(eventConsumer, times(1)).accept(eventCaptor.capture());
          Event event = eventCaptor.getValue();
          return EqualsBuilder.reflectionEquals(expectedEvent, event);
        });
  }

  private String testTopic() {
    return "topic_" + testName.getMethodName();
  }

  // XXX: Remove this method when merging into stable-3.3, since waitUntil is
  // available in Gerrit core.
  static void waitUntil(Supplier<Boolean> waitCondition) throws InterruptedException {
    Stopwatch stopwatch = Stopwatch.createStarted();
    while (!waitCondition.get()) {
      if (stopwatch.elapsed().compareTo(WAIT_PATIENCE) > 0) {
        throw new InterruptedException();
      }
      MILLISECONDS.sleep(50);
    }
  }

  static void waitUntilUnchecked(Supplier<Boolean> waitCondition) throws InterruptedException {
    try {
      waitUntil(uncheckedResultOf(waitCondition));
    } catch (InterruptedException e) {
      assertThat(waitCondition.get()).isTrue();
    }
  }

  static Supplier<Boolean> uncheckedResultOf(Supplier<Boolean> lambda) {
    return () -> {
      try {
        return lambda.get();
      } catch (Throwable e) {
        return false;
      }
    };
  }
}
