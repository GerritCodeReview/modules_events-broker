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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.plugins.Plugin;
import org.junit.Before;
import org.junit.Test;

public class BrokerApiPluginLoadedNotifierTest {
  private static final String BROKER_PLUGIN = "a-broker-plugin";
  private static final String LISTENER_PLUGIN = "a-listener-plugin";

  private DynamicItem<BrokerApi> brokerApiItem;
  private DynamicSet<BrokerApiLoadedListener> listeners;
  private BrokerApiPluginLoadedNotifier notifierUnderTest;

  @Before
  public void setup() {
    brokerApiItem = DynamicItem.itemOf(BrokerApi.class, new InProcessBrokerApi());
    listeners = DynamicSet.emptySet();
    notifierUnderTest = new BrokerApiPluginLoadedNotifier(brokerApiItem, listeners);
  }

  @Test
  public void shouldNotReportABoundBrokerWhenOnlyThePlaceholderIsAvailable() {
    assertThat(notifierUnderTest.boundBrokerApi()).isEmpty();
  }

  @Test
  public void shouldReportTheBrokerBoundByAPlugin() {
    BrokerApi pluginBroker = bindBroker(BROKER_PLUGIN);

    assertThat(notifierUnderTest.boundBrokerApi()).hasValue(pluginBroker);
  }

  @Test
  public void shouldNotifyRegisteredListenersWhenTheBrokerPluginStarts() {
    BrokerApiLoadedListener listener = register();
    bindBroker(BROKER_PLUGIN);

    notifierUnderTest.onStartPlugin(pluginNamed(BROKER_PLUGIN));

    verify(listener).brokerApiLoaded();
  }

  @Test
  public void shouldNotNotifyListenersWhenAPluginBindingNoBrokerStarts() {
    BrokerApiLoadedListener listener = register();
    bindBroker(BROKER_PLUGIN);

    notifierUnderTest.onStartPlugin(pluginNamed("another-plugin"));

    verify(listener, never()).brokerApiLoaded();
  }

  @Test
  public void shouldNotNotifyListenersWhenOnlyThePlaceholderIsAvailable() {
    BrokerApiLoadedListener listener = register();

    notifierUnderTest.onStartPlugin(pluginNamed(BROKER_PLUGIN));

    verify(listener, never()).brokerApiLoaded();
  }

  @Test
  public void shouldNotPropagateListenerFailures() {
    BrokerApiLoadedListener failingListener = register();
    doThrow(new IllegalStateException("listener failed")).when(failingListener).brokerApiLoaded();
    BrokerApiLoadedListener listener = register();
    bindBroker(BROKER_PLUGIN);

    notifierUnderTest.onStartPlugin(pluginNamed(BROKER_PLUGIN));

    verify(listener).brokerApiLoaded();
  }

  private BrokerApiLoadedListener register() {
    BrokerApiLoadedListener listener = mock(BrokerApiLoadedListener.class);
    @SuppressWarnings("unused")
    var unused = listeners.add(LISTENER_PLUGIN, listener);
    return listener;
  }

  private BrokerApi bindBroker(String pluginName) {
    BrokerApi pluginBroker = mock(BrokerApi.class);
    @SuppressWarnings("unused")
    var unused = brokerApiItem.set(pluginBroker, pluginName);
    return pluginBroker;
  }

  private Plugin pluginNamed(String pluginName) {
    Plugin plugin = mock(Plugin.class);
    when(plugin.getName()).thenReturn(pluginName);
    return plugin;
  }
}
