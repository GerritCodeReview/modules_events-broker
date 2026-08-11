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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Notifies interested parties when a broker plugin has bound its {@link BrokerApi}.
 *
 * <p>Broker plugins are loaded after the libraries consuming them, so subscribing during a
 * library's own startup would target {@link InProcessBrokerApi} rather than the real broker.
 * Consumers register here instead and subscribe once {@link #isBrokerPluginBound()} is true.
 */
@Singleton
public class BrokerApiBoundNotifier {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private final DynamicItem<BrokerApi> brokerApi;
  private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

  @Inject
  BrokerApiBoundNotifier(DynamicItem<BrokerApi> brokerApi) {
    this.brokerApi = brokerApi;
  }

  /** Returns true when a broker plugin, rather than the in-process placeholder, is bound. */
  public boolean isBrokerPluginBound() {
    return !PluginName.GERRIT.equals(brokerApi.getPluginName());
  }

  public void addListener(Runnable listener) {
    listeners.add(listener);
    log.atFine().log(
        "[broker-bound-trace] listener registered, %d now listening", listeners.size());
  }

  public void removeListener(Runnable listener) {
    listeners.remove(listener);
    log.atFine().log(
        "[broker-bound-trace] listener deregistered, %d still listening", listeners.size());
  }

  void fire() {
    log.atFine().log(
        "[broker-bound-trace] firing %d listener(s), bound broker provided by plugin [%s],"
            + " isBrokerPluginBound=%s",
        listeners.size(), brokerApi.getPluginName(), isBrokerPluginBound());
    for (Runnable listener : listeners) {
      try {
        listener.run();
      } catch (RuntimeException e) {
        log.atSevere().withCause(e).log("Listener failed to react to a broker being bound");
      }
    }
  }
}
