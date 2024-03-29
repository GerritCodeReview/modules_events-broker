// Copyright (C) 2019 The Android Open Source Project
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

import com.gerritforge.gerrit.eventbroker.log.Log4jMessageLogger;
import com.gerritforge.gerrit.eventbroker.log.MessageLogger;
import com.gerritforge.gerrit.eventbroker.metrics.BrokerMetrics;
import com.gerritforge.gerrit.eventbroker.metrics.BrokerMetricsNoOp;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.inject.Inject;
import com.google.inject.Scopes;

public class BrokerApiModule extends LifecycleModule {
  DynamicItem<BrokerApi> currentBrokerApi;

  @Inject(optional = true)
  public void setPreviousBrokerApi(DynamicItem<BrokerApi> currentBrokerApi) {
    this.currentBrokerApi = currentBrokerApi;
  }

  @Override
  protected void configure() {
    if (currentBrokerApi == null) {
      DynamicItem.itemOf(binder(), BrokerApi.class);
      DynamicItem.bind(binder(), BrokerApi.class).to(InProcessBrokerApi.class).in(Scopes.SINGLETON);
    }

    DynamicItem.itemOf(binder(), BrokerMetrics.class);
    DynamicItem.bind(binder(), BrokerMetrics.class)
        .to(BrokerMetricsNoOp.class)
        .in(Scopes.SINGLETON);

    bind(EventDeserializer.class).in(Scopes.SINGLETON);

    listener().to(Log4jMessageLogger.class);
    bind(MessageLogger.class).to(Log4jMessageLogger.class);
  }
}
