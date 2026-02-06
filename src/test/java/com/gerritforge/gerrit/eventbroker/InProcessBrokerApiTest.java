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
import static com.google.gerrit.testing.GerritJUnit.assertThrows;

import com.google.gerrit.server.events.Event;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;

public class InProcessBrokerApiTest {

  public static final int SEND_FUTURE_TIMEOUT = 1;
  Consumer<Event> eventConsumer;

  BrokerApi brokerApiUnderTest;
  UUID instanceId = UUID.randomUUID();

  @Before
  public void setup() {
    brokerApiUnderTest = new InProcessBrokerApi();
    eventConsumer = mockEventConsumer();
  }

  @Test
  public void sendEventShouldNotBeSupported() {
    assertThrows(UnsupportedOperationException.class, () -> brokerApiUnderTest.send("topic", null));
  }

  @Test
  public void shouldRegisterConsumerPerTopic() {
    Consumer<Event> secondConsumer = mockEventConsumer();
    brokerApiUnderTest.receiveAsync("topic", eventConsumer);
    brokerApiUnderTest.receiveAsync("topic2", secondConsumer);
    assertThat(brokerApiUnderTest.topicSubscribers().size()).isEqualTo(2);
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
  public void shouldDeliverAsynchronouslyEventToAllRegisteredConsumers() {
    Consumer<Event> secondConsumer = mockEventConsumer();
    brokerApiUnderTest.receiveAsync("topic", eventConsumer);
    brokerApiUnderTest.receiveAsync("topic", secondConsumer);
    assertThat(brokerApiUnderTest.topicSubscribers().size()).isEqualTo(2);
  }

  @Test
  public void shouldNotRegisterTheSameConsumerTwicePerTopic() {
    brokerApiUnderTest.receiveAsync("topic", eventConsumer);
    brokerApiUnderTest.receiveAsync("topic", eventConsumer);

    assertThat(brokerApiUnderTest.topicSubscribers().size()).isEqualTo(1);
  }

  @Test
  public void shouldReconnectSubscribers() {
    brokerApiUnderTest.receiveAsync("topic", eventConsumer);
    assertThat(brokerApiUnderTest.topicSubscribers()).isNotEmpty();

    Consumer<Event> newConsumer = mockEventConsumer();

    brokerApiUnderTest.disconnect();
    assertThat(brokerApiUnderTest.topicSubscribers()).isEmpty();

    brokerApiUnderTest.receiveAsync("topic", newConsumer);
    assertThat(brokerApiUnderTest.topicSubscribers()).isNotEmpty();
  }

  @Test
  public void shouldDisconnectSubscribers() {
    brokerApiUnderTest.receiveAsync("topic", eventConsumer);
    brokerApiUnderTest.receiveAsyncWithContext("topic2", dummyContextAwareConsumer());
    brokerApiUnderTest.receiveAsyncWithContext("topic3", "group-id", dummyContextAwareConsumer());
    brokerApiUnderTest.disconnect();
    assertThat(brokerApiUnderTest.topicSubscribers()).isEmpty();
    assertThat(brokerApiUnderTest.topicSubscribersWithContext()).isEmpty();
    assertThat(brokerApiUnderTest.topicSubscribersWithContextAndGroupId()).isEmpty();
  }

  @Test
  public void replayAllEventsShouldNotBeSupported() {
    assertThrows(
        UnsupportedOperationException.class, () -> brokerApiUnderTest.replayAllEvents("topic"));
  }

  @Test
  public void shouldRegisterContextAwareConsumerPerTopic() {
    brokerApiUnderTest.receiveAsyncWithContext("topic", dummyContextAwareConsumer());
    brokerApiUnderTest.receiveAsyncWithContext("topic2", dummyContextAwareConsumer());
    assertThat(brokerApiUnderTest.topicSubscribersWithContext().size()).isEqualTo(2);
  }

  @Test
  public void shouldNotRegisterTheSameContextConsumerTwicePerTopic() {
    ContextAwareSubscriber<Event> contextAwareConsumer = dummyContextAwareConsumer();
    brokerApiUnderTest.receiveAsyncWithContext("topic", contextAwareConsumer);
    brokerApiUnderTest.receiveAsyncWithContext("topic", contextAwareConsumer);

    assertThat(brokerApiUnderTest.topicSubscribersWithContext().size()).isEqualTo(1);
  }

  @Test
  public void shouldAllowSameContextConsumerForSameTopicButDifferentGroupId() {
    brokerApiUnderTest.receiveAsyncWithContext("topic", "group1", dummyContextAwareConsumer());
    brokerApiUnderTest.receiveAsyncWithContext("topic2", "group2", dummyContextAwareConsumer());
    assertThat(brokerApiUnderTest.topicSubscribersWithContextAndGroupId().size()).isEqualTo(2);
  }

  @Test
  public void shouldNotRegisterTheSameContextConsumerTwicePerTopicWithGroupId() {
    ContextAwareSubscriber<Event> contextAwareConsumer = dummyContextAwareConsumer();
    brokerApiUnderTest.receiveAsyncWithContext("topic", "group1", contextAwareConsumer);
    brokerApiUnderTest.receiveAsyncWithContext("topic", "group1", contextAwareConsumer);

    assertThat(brokerApiUnderTest.topicSubscribersWithContextAndGroupId()).hasSize(1);
  }

  private static class Subscriber<T> implements Consumer<T> {

    @Override
    public void accept(T eventMessage) {}
  }

  private static class ContextAwareSubscriber<T> implements ContextAwareConsumer<T> {

    @Override
    public void accept(T t, MessageContext ctx) {}
  }

  private <T> Consumer<T> mockEventConsumer() {
    return new Subscriber<>();
  }

  private <T> ContextAwareSubscriber<T> dummyContextAwareConsumer() {
    return new ContextAwareSubscriber<>();
  }
}
