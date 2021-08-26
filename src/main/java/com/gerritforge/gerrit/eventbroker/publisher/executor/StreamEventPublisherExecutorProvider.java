// Copyright (C) 2021 The Android Open Source Project
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

package com.gerritforge.gerrit.eventbroker.publisher.executor;

import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.util.concurrent.ScheduledExecutorService;

@Singleton
public class StreamEventPublisherExecutorProvider implements Provider<ScheduledExecutorService> {

  private ScheduledExecutorService executor;

  @Inject
  public StreamEventPublisherExecutorProvider(WorkQueue workQueue) {
    executor = workQueue.createQueue(1, "Forward-Stream-Event");
  }

  public void start() {
    // do nothing
  }

  public void stop() {
    executor.shutdown();
    executor = null;
  }

  @Override
  public ScheduledExecutorService get() {
    return executor;
  }
}
