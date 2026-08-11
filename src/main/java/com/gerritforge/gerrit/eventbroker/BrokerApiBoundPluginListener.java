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
import com.google.gerrit.server.plugins.Plugin;
import com.google.gerrit.server.plugins.StartPluginListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// PluginGuiceEnvironment broadcasts this only after attaching the plugin's DynamicItems, so a
// broker plugin's BrokerApi is already bound by the time listeners run.
@Singleton
class BrokerApiBoundPluginListener implements StartPluginListener {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private final BrokerApiBoundNotifier notifier;

  @Inject
  BrokerApiBoundPluginListener(BrokerApiBoundNotifier notifier) {
    this.notifier = notifier;
  }

  @Override
  public void onStartPlugin(Plugin plugin) {
    log.atFine().log("[broker-bound-trace] plugin [%s] started", plugin.getName());
    notifier.fire();
  }
}
