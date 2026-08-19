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
 * Notifies interested parties when a plugin is loaded that provides a {@link DynamicItem} of {@link
 * BrokerApi}.
 */
public interface StartBrokerApiPluginListener extends BrokerApiPluginListener, StartPluginListener {

  /** Invoked once a broker plugin has bound its {@link BrokerApi}. */
  void onStartBrokerApiPlugin(Plugin plugin);

  @Override
  default void onStartPlugin(Plugin plugin) {
    if (isBrokerApiPlugin(plugin)) {
      onStartBrokerApiPlugin(plugin);
    }
  }
}
