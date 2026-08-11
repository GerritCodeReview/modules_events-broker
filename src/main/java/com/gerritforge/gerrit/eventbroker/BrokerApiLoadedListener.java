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

/**
 * Listens for a {@link BrokerApi} being bound by a broker plugin.
 *
 * <p>Implementations register by binding themselves into the {@code
 * DynamicSet<BrokerApiLoadedListener>} declared by {@link BrokerApiModule}, so that they know when
 * a real broker api is bound.
 *
 * <p>Registering does not invoke the listener, and a broker plugin loaded before the listener fires
 * no notification, so a listener must also consult {@link
 * BrokerApiPluginLoadedNotifier#boundBrokerApi()} itself on startup.
 */
public interface BrokerApiLoadedListener {

  /** Invoked once a broker plugin has bound its {@link BrokerApi}. */
  void brokerApiLoaded();
}
