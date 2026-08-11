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
import static org.mockito.Mockito.mock;

import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.extensions.registration.RegistrationHandle;
import com.google.gerrit.server.plugins.Plugin;
import org.junit.Before;
import org.junit.Test;

public class BrokerApiPluginListenerTest {
  private static final String BROKER_PLUGIN = "a-broker-plugin";

  private DynamicItem<BrokerApi> brokerApiItem;
  private RegistrationHandle brokerHandle;
  private TestListener listenerUnderTest;

  private static class TestListener implements BrokerApiPluginListener {
    private final DynamicItem<BrokerApi> brokerApiItem;
    private RuntimeException failure;
    int notifications;

    TestListener(DynamicItem<BrokerApi> brokerApiItem) {
      this.brokerApiItem = brokerApiItem;
    }

    @Override
    public DynamicItem<BrokerApi> brokerApiDynamicItem() {
      return brokerApiItem;
    }

    @Override
    public void onBrokerApiChanged() {
      notifications++;
      if (failure != null) {
        throw failure;
      }
    }

    void failWith(RuntimeException e) {
      failure = e;
    }
  }

  @Before
  public void setup() {
    brokerApiItem = DynamicItem.itemOf(BrokerApi.class, new InProcessBrokerApi());
    listenerUnderTest = new TestListener(brokerApiItem);
  }

  @Test
  public void shouldNotReportABoundBrokerWhenOnlyThePlaceholderIsAvailable() {
    assertThat(listenerUnderTest.isBrokerApiBound()).isFalse();
  }

  @Test
  public void shouldReportTheBrokerBoundByAPlugin() {
    bindBroker(BROKER_PLUGIN);

    assertThat(listenerUnderTest.isBrokerApiBound()).isTrue();
  }

  @Test
  public void shouldNotReportABoundBrokerOnceThePluginProvidingItIsUnloaded() {
    bindBroker(BROKER_PLUGIN);
    brokerHandle.remove();

    assertThat(listenerUnderTest.isBrokerApiBound()).isFalse();
  }

  @Test
  public void shouldNotifyWhenAnyPluginStarts() {
    listenerUnderTest.onStartPlugin(mock(Plugin.class));

    assertThat(listenerUnderTest.notifications).isEqualTo(1);
  }

  @Test
  public void shouldNotifyWhenAnyPluginStops() {
    listenerUnderTest.onStopPlugin(mock(Plugin.class));

    assertThat(listenerUnderTest.notifications).isEqualTo(1);
  }

  @Test
  public void shouldNotPropagateListenerFailures() {
    listenerUnderTest.failWith(new IllegalStateException("listener failed"));

    listenerUnderTest.onStartPlugin(mock(Plugin.class));
    listenerUnderTest.onStopPlugin(mock(Plugin.class));

    assertThat(listenerUnderTest.notifications).isEqualTo(2);
  }

  private void bindBroker(String pluginName) {
    brokerHandle = brokerApiItem.set(mock(BrokerApi.class), pluginName);
  }
}
