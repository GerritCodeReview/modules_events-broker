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

import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.plugins.Plugin;
import com.google.gerrit.server.plugins.StartPluginListener;

/**
 * Reacts to the {@link BrokerApi} bound into a {@link DynamicItem} being provided by a plugin.
 *
 * <p>Implementations bind themselves as {@link StartPluginListener} and are called back through
 * {@link #onBrokerApiStarted()} only for the plugin that provides the {@link BrokerApi}.
 *
 * <p>Broker plugins shut their own subscriptions down when they stop, so no withdrawal callback is
 * offered.
 *
 * <p>No callback is fired for a broker plugin that was already loaded when the implementation was
 * registered, so implementations must also consult {@link #isBrokerApiStarted()} on startup.
 */
public interface BrokerApiPluginListener extends StartPluginListener {

  /** Returns the item a broker plugin binds its {@link BrokerApi} into. */
  DynamicItem<BrokerApi> brokerApiDynamicItem();

  void onBrokerApiStarted();

  default boolean isBrokerApiStarted() {
    DynamicItem<BrokerApi> item = brokerApiDynamicItem();
    return item != null && item.get() != null;
  }

  @Override
  default void onStartPlugin(Plugin plugin) {
    if (bindsBrokerApi(plugin)) {
      onBrokerApiStarted();
    }
  }

  private boolean bindsBrokerApi(Plugin plugin) {
    return plugin.getName().equals(brokerApiDynamicItem().getPluginName());
  }
}
