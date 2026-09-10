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

import com.gerritforge.gerrit.eventbroker.log.MessageLogger;
import com.google.common.flogger.FluentLogger;
import com.google.inject.Inject;

/**
 * {@link BrokerApi} delegate that logs to the message log the events sent to the broker.
 * <p>NOTE: The messages that failed to be sent/received/acknowledged are not logged
 * into the message log, but their failure is logged at `severe` level in the
 * Gerrit's error_log.
*/
public class BrokerApiLoggingListener implements BrokerApiMessageListener {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private final MessageLogger msgLog;

  @Inject
  public BrokerApiLoggingListener(MessageLogger msgLog) {
    this.msgLog = msgLog;
  }

  @Override
  public void messageProcessed(MessageLogger.Direction direction, String topic, Object message) {
    msgLog.log(direction, topic, message);
  }

  @Override
  public void messageFailed(
      MessageLogger.Direction direction, String topic, Object message, Throwable e) {
    log.atSevere().withCause(e).log(
        "Failed to %s message '%s' to topic '%s'", direction, message, topic);
  }
}
