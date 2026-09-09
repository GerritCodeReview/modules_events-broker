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
import com.google.gerrit.server.events.Event;

/** API for sending/receiving events through a message Broker. */
public interface BrokerApiMessageListener {

  /**
   * Message has been processed to/from a topic successfully.
   *
   * @param topic topic name
   * @param message sent/requeued/received to/from the topic
   */
  void messageProcessed(MessageLogger.Direction direction, String topic, Object message);

  /**
   * Message failed to be processed to/from a topic.
   *
   * @param topic topic name
   * @param message sent/requeued/received failed to/from the topic
   * @param e exception that was raised when the message failed
   */
  void messageFailed(MessageLogger.Direction direction, String topic, Object message, Throwable e);
}
