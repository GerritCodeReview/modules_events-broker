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

/**
 * Reacts to the {@link BrokerApi} bound into a {@link DynamicItem} being provided or withdrawn by a
 * plugin.
 *
 * <p>Implementations bind themselves as {@link StartPluginListener} and {@link StopPluginListener}
 * and are called back through {@link #onBrokerApiChanged()} whenever a plugin is started or
 * stopped.
 *
 * <p>Plugin reload is deliberately not handled: reloading a plugin does not unload it first, so the
 * state of a broker plugin across a reload is undefined and is not supported.
 *
 * <p>No callback is fired for a broker plugin that was already loaded when the implementation was
 * registered, so implementations must also consult {@link #isBrokerApiBound()} on startup.
 */
public interface BrokerApiPluginListener extends StartPluginListener, StopPluginListener {
  FluentLogger log = FluentLogger.forEnclosingClass();

  /** Returns the item a broker plugin binds its {@link BrokerApi} into. */
  DynamicItem<BrokerApi> brokerApiDynamicItem();

  /** Invoked when the {@link BrokerApi} bound by a plugin may have changed. */
  void onBrokerApiChanged();

  /**
   * Returns true when a plugin has bound a {@link BrokerApi}, and false when only the in-process
   * placeholder provided by Gerrit itself is available.
   */
  default boolean isBrokerApiBound() {
    DynamicItem<BrokerApi> item = brokerApiDynamicItem();
    return !PluginName.GERRIT.equals(item.getPluginName()) && item.get() != null;
  }

  @Override
  default void onStartPlugin(Plugin plugin) {
    notifyBrokerApiChanged();
  }

  @Override
  default void onStopPlugin(Plugin plugin) {
    notifyBrokerApiChanged();
  }

  /**
   * Gerrit fans out plugin lifecycle events without isolating failures, so an exception escaping
   * here would break the start of the plugin that triggered the notification.
   */
  private void notifyBrokerApiChanged() {
    try {
      onBrokerApiChanged();
    } catch (RuntimeException e) {
      log.atSevere().withCause(e).log(
          "Listener failed to react to a change of the bound BrokerApi");
    }
  }
}
