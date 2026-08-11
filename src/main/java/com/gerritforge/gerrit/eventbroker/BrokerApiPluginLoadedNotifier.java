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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.registration.PluginName;
import com.google.gerrit.server.plugins.Plugin;
import com.google.gerrit.server.plugins.StartPluginListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;

/** Notifies interested parties when a broker plugin has bound its {@link BrokerApi}. */
@Singleton
public class BrokerApiPluginLoadedNotifier implements StartPluginListener {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private final DynamicItem<BrokerApi> brokerApi;
  private final DynamicSet<BrokerApiLoadedListener> listeners;

  @Inject
  BrokerApiPluginLoadedNotifier(
      DynamicItem<BrokerApi> brokerApi, DynamicSet<BrokerApiLoadedListener> listeners) {
    this.brokerApi = brokerApi;
    this.listeners = listeners;
  }

  /**
   * Returns the {@link BrokerApi} a broker plugin has bound, or empty when only the in-process
   * placeholder is available.
   */
  public Optional<BrokerApi> boundBrokerApi() {
    return PluginName.GERRIT.equals(brokerApi.getPluginName())
        ? Optional.empty()
        : Optional.ofNullable(brokerApi.get());
  }

  /**
   * Returns true when the events broker currently bound is the one provided by the given plugin.
   *
   * <p>{@link DynamicItem#getPluginName()} reports whichever plugin bound the item, a name Gerrit
   * records as it attaches that plugin's DynamicItems. The comparison therefore identifies a broker
   * by the registration it made rather than by how it is named: a plugin binding a {@link
   * BrokerApi} matches whatever it is called, and one binding none never matches.
   */
  private boolean isEventsBrokerBoundBy(String pluginName) {
    return pluginName.equals(brokerApi.getPluginName());
  }

  /**
   * Notifies every registered listener that a broker plugin has bound its {@link BrokerApi}.
   *
   * <p>Exceptions thrown while notifying a listener are logged rather than propagated, so that a
   * failing listener cannot disrupt the start of the plugin that triggered the notification.
   */
  private void fire() {
    log.atFine().log(
        "[broker-bound-trace] plugin [%s] bound %s, firing listeners",
        brokerApi.getPluginName(), boundBrokerApi().orElse(null));
    for (BrokerApiLoadedListener listener : listeners) {
      try {
        listener.brokerApiLoaded();
      } catch (RuntimeException e) {
        log.atSevere().withCause(e).log("Listener failed to react to a broker being bound");
      }
    }
  }

  @Override
  public void onStartPlugin(Plugin plugin) {
    log.atFine().log("[broker-bound-trace] plugin [%s] started", plugin.getName());
    if (isEventsBrokerBoundBy(plugin.getName())) {
      fire();
    }
  }
}
