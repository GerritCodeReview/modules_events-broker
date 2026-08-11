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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.gerrit.extensions.registration.DynamicItem;
import org.junit.Before;
import org.junit.Test;

public class BrokerApiBoundNotifierTest {
  private static final String BROKER_PLUGIN = "a-broker-plugin";

  private DynamicItem<BrokerApi> brokerApiItem;
  private BrokerApiBoundNotifier notifierUnderTest;

  @Before
  public void setup() {
    brokerApiItem = DynamicItem.itemOf(BrokerApi.class, new InProcessBrokerApi());
    notifierUnderTest = new BrokerApiBoundNotifier(brokerApiItem);
  }

  @Test
  public void shouldNotReportABoundBrokerWhenOnlyThePlaceholderIsAvailable() {
    assertThat(notifierUnderTest.boundBrokerApi()).isEmpty();
    assertThat(notifierUnderTest.isEventsBrokerBoundBy(BROKER_PLUGIN)).isFalse();
  }

  @Test
  public void shouldReportTheBrokerBoundByAPlugin() {
    BrokerApi pluginBroker = bindBroker(BROKER_PLUGIN);

    assertThat(notifierUnderTest.boundBrokerApi()).hasValue(pluginBroker);
    assertThat(notifierUnderTest.isEventsBrokerBoundBy(BROKER_PLUGIN)).isTrue();
    assertThat(notifierUnderTest.isEventsBrokerBoundBy("another-plugin")).isFalse();
  }

  @Test
  public void shouldNotifyRegisteredSubscribers() {
    BrokerApiSubscriber subscriber = mock(BrokerApiSubscriber.class);
    notifierUnderTest.addListener(subscriber);

    notifierUnderTest.fire();

    verify(subscriber).subscribe();
  }

  @Test
  public void shouldNotNotifySubscribersOnceRemoved() {
    BrokerApiSubscriber subscriber = mock(BrokerApiSubscriber.class);
    notifierUnderTest.addListener(subscriber);
    notifierUnderTest.fire();

    notifierUnderTest.removeListener(subscriber);
    notifierUnderTest.fire();

    verify(subscriber, times(1)).subscribe();
  }

  @Test
  public void shouldNotPropagateSubscriberFailures() {
    BrokerApiSubscriber failingSubscriber = mock(BrokerApiSubscriber.class);
    doThrow(new IllegalStateException("subscriber failed")).when(failingSubscriber).subscribe();
    BrokerApiSubscriber subscriber = mock(BrokerApiSubscriber.class);
    notifierUnderTest.addListener(failingSubscriber);
    notifierUnderTest.addListener(subscriber);

    notifierUnderTest.fire();

    verify(subscriber).subscribe();
  }

  private BrokerApi bindBroker(String pluginName) {
    BrokerApi pluginBroker = mock(BrokerApi.class);
    @SuppressWarnings("unused")
    var unused = brokerApiItem.set(pluginBroker, pluginName);
    return pluginBroker;
  }
}
