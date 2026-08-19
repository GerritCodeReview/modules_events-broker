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
import com.google.gerrit.server.plugins.Plugin;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class BrokerApiPluginLoadedNotifierTest {
  private static final String BROKER_PLUGIN = "a-broker-plugin";

  private DynamicItem<BrokerApi> brokerApiItem;
  private RecordingNotifier notifierUnderTest;

  private static class RecordingNotifier implements BrokerApiPluginLoadedNotifier {
    private final DynamicItem<BrokerApi> brokerApi;
    private final List<BrokerApi> notified = new ArrayList<>();

    RecordingNotifier(DynamicItem<BrokerApi> brokerApi) {
      this.brokerApi = brokerApi;
    }

    @Override
    public DynamicItem<BrokerApi> boundBrokerApi() {
      return brokerApi;
    }

    @Override
    public void onBrokerApiStartPlugin(BrokerApi api) {
      notified.add(api);
    }
  }

  @Before
  public void setup() {
    brokerApiItem = DynamicItem.itemOf(BrokerApi.class, new InProcessBrokerApi());
    notifierUnderTest = new RecordingNotifier(brokerApiItem);
  }

  @Test
  public void shouldNotifyWhenTheBrokerPluginStarts() {
    BrokerApi pluginBroker = bindBroker(BROKER_PLUGIN);

    notifierUnderTest.onStartPlugin(pluginNamed(BROKER_PLUGIN));

    assertThat(notifierUnderTest.notified).containsExactly(pluginBroker);
  }

  @Test
  public void shouldNotNotifyWhenAPluginBindingNoBrokerStarts() {
    bindBroker(BROKER_PLUGIN);

    notifierUnderTest.onStartPlugin(pluginNamed("another-plugin"));

    assertThat(notifierUnderTest.notified).isEmpty();
  }

  @Test
  public void shouldNotNotifyWhenOnlyThePlaceholderIsAvailable() {
    notifierUnderTest.onStartPlugin(pluginNamed(BROKER_PLUGIN));

    assertThat(notifierUnderTest.notified).isEmpty();
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
