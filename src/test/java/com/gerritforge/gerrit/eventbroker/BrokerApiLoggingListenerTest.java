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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.gerritforge.gerrit.eventbroker.log.MessageLogger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BrokerApiLoggingListenerTest {
  private static final String TEST_TOPIC = "test-topic";
  private static final Object TEST_MESSAGE_PAYLOAD = "test-message-payload";
  private static final MessageLogger.Direction TEST_DIRECTION = MessageLogger.Direction.CONSUME;

  @Mock private MessageLogger messageLoggerMock;

  private BrokerApiLoggingListener listener;

  @Before
  public void setUp() {
    listener = new BrokerApiLoggingListener(messageLoggerMock);
  }

  @Test
  public void messageProcessed_logsMessageToMsgLog() {
    listener.messageProcessed(TEST_DIRECTION, TEST_TOPIC, TEST_MESSAGE_PAYLOAD);

    verify(messageLoggerMock).log(TEST_DIRECTION, TEST_TOPIC, TEST_MESSAGE_PAYLOAD);
    verifyNoMoreInteractions(messageLoggerMock);
  }

  @Test
  public void messageFailed_logsSevereErrorWithoutExceptions() {
    Exception exception = new Exception("Simulated broker failure");

    listener.messageFailed(TEST_DIRECTION, TEST_TOPIC, TEST_MESSAGE_PAYLOAD, exception);
    verifyNoMoreInteractions(messageLoggerMock);
  }
}
