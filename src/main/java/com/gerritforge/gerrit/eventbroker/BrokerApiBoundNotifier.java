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
import com.google.gerrit.extensions.registration.PluginName;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/** Notifies interested parties when a broker plugin has bound its {@link BrokerApi}. */
@Singleton
public class BrokerApiBoundNotifier {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private final DynamicItem<BrokerApi> brokerApi;
  private final List<BrokerApiSubscriber> listeners = new CopyOnWriteArrayList<>();

  @Inject
  BrokerApiBoundNotifier(DynamicItem<BrokerApi> brokerApi) {
    this.brokerApi = brokerApi;
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

  public void addListener(BrokerApiSubscriber listener) {
    listeners.add(listener);
    log.atFine().log(
        "[broker-bound-trace] listener registered, %d now listening", listeners.size());
  }

  public void removeListener(BrokerApiSubscriber listener) {
    listeners.remove(listener);
    log.atFine().log(
        "[broker-bound-trace] listener deregistered, %d still listening", listeners.size());
  }

  /**
   * Returns true when the events broker currently bound is the one provided by the given plugin.
   *
   * <p>{@link DynamicItem#getPluginName()} reports whichever plugin bound the item, a name Gerrit
   * records as it attaches that plugin's DynamicItems. The comparison therefore identifies a broker
   * by the registration it made rather than by how it is named: a plugin binding a {@link
   * BrokerApi} matches whatever it is called, and one binding none never matches.
   */
  boolean isEventsBrokerBoundBy(String pluginName) {
    return pluginName.equals(brokerApi.getPluginName());
  }

  void fire() {
    log.atFine().log(
        "[broker-bound-trace] plugin [%s] bound %s, firing %d listener(s)",
        brokerApi.getPluginName(), boundBrokerApi().orElse(null), listeners.size());
    for (BrokerApiSubscriber listener : listeners) {
      try {
        listener.subscribe();
      } catch (RuntimeException e) {
        log.atSevere().withCause(e).log("Listener failed to react to a broker being bound");
      }
    }
  }
}
