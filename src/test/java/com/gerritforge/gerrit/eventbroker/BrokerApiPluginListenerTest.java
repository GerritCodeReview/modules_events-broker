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
import static org.mockito.Mockito.when;

import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.extensions.registration.RegistrationHandle;
import com.google.gerrit.server.plugins.Plugin;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class BrokerApiPluginListenerTest {
  private static final String BROKER_PLUGIN = "a-broker-plugin";
  private static final String ANOTHER_PLUGIN = "another-plugin";

  private DynamicItem<BrokerApi> brokerApiItem;
  private RegistrationHandle brokerHandle;
  private TestListener listenerUnderTest;

  private static class TestListener implements BrokerApiPluginListener {
    private final DynamicItem<BrokerApi> brokerApiItem;
    private RuntimeException stoppedFailure;
    private String subscribedTo;
    int started;
    int stopped;

    TestListener(DynamicItem<BrokerApi> brokerApiItem) {
      this.brokerApiItem = brokerApiItem;
    }

    @Override
    public DynamicItem<BrokerApi> brokerApiDynamicItem() {
      return brokerApiItem;
    }

    @Override
    public Optional<String> subscribedBrokerApiPlugin() {
      return Optional.ofNullable(subscribedTo);
    }

    @Override
    public void onBrokerApiStarted() {
      started++;
      subscribedTo = brokerApiItem.getPluginName();
    }

    @Override
    public void onBrokerApiStopped() {
      stopped++;
      subscribedTo = null;
      if (stoppedFailure != null) {
        throw stoppedFailure;
      }
    }

    void failOnStoppedWith(RuntimeException e) {
      stoppedFailure = e;
    }
  }

  @Before
  public void setup() {
    brokerApiItem = DynamicItem.itemOf(BrokerApi.class, new InProcessBrokerApi());
    listenerUnderTest = new TestListener(brokerApiItem);
  }

  @Test
  public void shouldNotReportAStartedBrokerWhenOnlyThePlaceholderIsAvailable() {
    assertThat(listenerUnderTest.isBrokerApiStarted()).isFalse();
  }

  @Test
  public void shouldReportTheBrokerStartedByAPlugin() {
    bindBroker(BROKER_PLUGIN);

    assertThat(listenerUnderTest.isBrokerApiStarted()).isTrue();
  }

  @Test
  public void shouldNotReportAStartedBrokerOnceThePluginProvidingItIsUnloaded() {
    bindBroker(BROKER_PLUGIN);
    brokerHandle.remove();

    assertThat(listenerUnderTest.isBrokerApiStarted()).isFalse();
  }

  @Test
  public void shouldNotifyWhenThePluginBindingTheBrokerStarts() {
    bindBroker(BROKER_PLUGIN);

    listenerUnderTest.onStartPlugin(pluginNamed(BROKER_PLUGIN));

    assertThat(listenerUnderTest.started).isEqualTo(1);
  }

  @Test
  public void shouldNotNotifyWhenAPluginBindingNoBrokerStarts() {
    bindBroker(BROKER_PLUGIN);

    listenerUnderTest.onStartPlugin(pluginNamed(ANOTHER_PLUGIN));

    assertThat(listenerUnderTest.started).isEqualTo(0);
  }

  @Test
  public void shouldNotNotifyWhenOnlyThePlaceholderIsAvailable() {
    listenerUnderTest.onStartPlugin(pluginNamed(BROKER_PLUGIN));

    assertThat(listenerUnderTest.started).isEqualTo(0);
  }

  @Test
  public void shouldNotifyWhenTheSubscribedBrokerPluginStops() {
    bindBroker(BROKER_PLUGIN);
    listenerUnderTest.onStartPlugin(pluginNamed(BROKER_PLUGIN));
    brokerHandle.remove();

    listenerUnderTest.onStopPlugin(pluginNamed(BROKER_PLUGIN));

    assertThat(listenerUnderTest.stopped).isEqualTo(1);
  }

  @Test
  public void shouldNotNotifyWhenAPluginOtherThanTheBrokerStops() {
    bindBroker(BROKER_PLUGIN);
    listenerUnderTest.onStartPlugin(pluginNamed(BROKER_PLUGIN));

    listenerUnderTest.onStopPlugin(pluginNamed(ANOTHER_PLUGIN));

    assertThat(listenerUnderTest.stopped).isEqualTo(0);
  }

  @Test
  public void shouldNotPropagateListenerFailuresWhenTheBrokerPluginStops() {
    bindBroker(BROKER_PLUGIN);
    listenerUnderTest.onStartPlugin(pluginNamed(BROKER_PLUGIN));
    brokerHandle.remove();
    listenerUnderTest.failOnStoppedWith(new IllegalStateException("listener failed"));

    listenerUnderTest.onStopPlugin(pluginNamed(BROKER_PLUGIN));

    assertThat(listenerUnderTest.stopped).isEqualTo(1);
  }

  @Test
  public void shouldNotNotifyWhenNoBrokerWasEverSubscribedTo() {
    listenerUnderTest.onStopPlugin(pluginNamed(BROKER_PLUGIN));

    assertThat(listenerUnderTest.stopped).isEqualTo(0);
  }

  private void bindBroker(String pluginName) {
    brokerHandle = brokerApiItem.set(mock(BrokerApi.class), pluginName);
  }

  private Plugin pluginNamed(String pluginName) {
    Plugin plugin = mock(Plugin.class);
    when(plugin.getName()).thenReturn(pluginName);
    return plugin;
  }
}
