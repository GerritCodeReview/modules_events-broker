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
import com.google.gerrit.server.plugins.Plugin;
import com.google.gerrit.server.plugins.StartPluginListener;
import com.google.gerrit.server.plugins.StopPluginListener;
import java.util.Optional;

/**
 * Reacts to the {@link BrokerApi} bound into a {@link DynamicItem} being provided or withdrawn by a
 * plugin.
 *
 * <p>Implementations bind themselves as {@link StartPluginListener} and {@link StopPluginListener}
 * and are called back through {@link #onBrokerApiStarted()} and {@link #onBrokerApiStopped()} only
 * for the plugin that provides the {@link BrokerApi}.
 *
 * <p>Plugin reload is deliberately not handled, brokers need to implement ReloadMode: restart.
 * Default reload mode for plugins does not unload them first, so the state of a broker plugin
 * across a reload is undefined, therefore we don't supported.
 *
 * <p>No callback is fired for a broker plugin that was already loaded when the implementation was
 * registered, so implementations must also consult {@link #isBrokerApiStarted()} on startup.
 */
public interface BrokerApiPluginListener extends StartPluginListener, StopPluginListener {
  FluentLogger log = FluentLogger.forEnclosingClass();

  /** Returns the item a broker plugin binds its {@link BrokerApi} into. */
  DynamicItem<BrokerApi> brokerApiDynamicItem();

  /** Returns the name of the broker plugin currently being consumed from, if any. */
  Optional<String> subscribedBrokerApiPlugin();

  void onBrokerApiStarted();

  void onBrokerApiStopped();

  default boolean isBrokerApiStarted() {
    DynamicItem<BrokerApi> item = brokerApiDynamicItem();
    return !PluginName.GERRIT.equals(item.getPluginName()) && item.get() != null;
  }

  @Override
  default void onStartPlugin(Plugin plugin) {
    if (bindsBrokerApi(plugin)) {
      onBrokerApiStarted();
    }
  }

  @Override
  default void onStopPlugin(Plugin plugin) {
    if (providesSubscribedBrokerApi(plugin)) {
      try {
        onBrokerApiStopped();
      } catch (RuntimeException e) {
        log.atSevere().withCause(e).log(
            "Listener failed to unsubscribe when Broker plugin %s stopped.",
            plugin.getName());
      }
    }
  }

  private boolean bindsBrokerApi(Plugin plugin) {
    return plugin.getName().equals(brokerApiDynamicItem().getPluginName());
  }

  private boolean providesSubscribedBrokerApi(Plugin plugin) {
    return subscribedBrokerApiPlugin().filter(plugin.getName()::equals).isPresent();
  }
}
