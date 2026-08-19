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

public interface BrokerApiPluginListener {

  /** Returns the item a broker plugin binds its {@link BrokerApi} into. */
  DynamicItem<BrokerApi> brokerApiDynamicItem();

  /**
   * Returns true if the plugin exposes a {@link DynamicItem} of {@link BrokerApi}.
   *
   * @param plugin plugin to check
   * @return true if plugin exposes a {@link DynamicItem} of {@link BrokerApi}
   */
  default boolean isBrokerApiPlugin(Plugin plugin) {
    return plugin.getName().equals(brokerApiDynamicItem().getPluginName());
  }
}
