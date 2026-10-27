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

import static com.google.common.truth.Truth.assertThat;

import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventGsonProvider;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

public class EventDeserializerTest {
  private EventDeserializer deserializer;
  private EventSerializer serializer;

  @Before
  public void setUp() {
    final Gson gson = new EventGsonProvider().get();
    deserializer = new EventDeserializer(gson);
    serializer = new EventSerializer(gson);
  }

  @Test
  public void deserializeMessageShouldReturnEmptyMetadataWhenAbsent() {
    final String eventJson = "{ \"type\": \"project-created\", \"instanceId\":\"instance-id\" }";
    final EventDeserializer.Message message = deserializer.deserializeMessage(eventJson);

    assertThat(message.event().instanceId).isEqualTo("instance-id");
    assertThat(message.metadata()).isEqualTo(MessageMetadata.EMPTY);
  }

  @Test
  public void deserializeMessageShouldReadMetadataWrittenBySerializer() {
    ProjectCreatedEvent event = new ProjectCreatedEvent();
    event.instanceId = "instance-id";
    MessageMetadata metadata = MessageMetadata.EMPTY.nextRetry("retrying-instance-id");

    EventDeserializer.Message message =
        deserializer.deserializeMessage(serializer.serialize(event, metadata));

    assertThat(message.event().instanceId).isEqualTo("instance-id");
    assertThat(message.metadata()).isEqualTo(metadata);
  }

  @Test
  public void nextRetryShouldIncrementRetryCount() {
    MessageMetadata metadata =
        MessageMetadata.EMPTY.nextRetry("first-instance").nextRetry("second-instance");

    assertThat(metadata.retry().retryCount()).isEqualTo(2);
    assertThat(metadata.retry().retryingInstanceId()).isEqualTo("second-instance");
  }

  @Test
  public void eventDeserializerShouldParseEvent() {
    final String eventJson = "{ \"type\": \"project-created\", \"instanceId\":\"instance-id\" }";
    final Event event = deserializer.deserialize(eventJson);

    assertThat(event.instanceId).isEqualTo("instance-id");
  }

  @Test
  public void eventDeserializerShouldParseEventWithoutInstanceId() {
    final String eventJson = "{ \"type\": \"project-created\" }";
    final Event event = deserializer.deserialize(eventJson);

    assertThat(event.instanceId).isNull();
  }

  @Test
  public void eventDeserializerShouldParseEventWhenInstanceIdIsEmpty() {
    final String eventJson = "{ \"type\": \"project-created\", \"instanceId\":\"\" }";
    final Event event = deserializer.deserialize(eventJson);

    assertThat(event.instanceId).isEmpty();
  }

  @Test
  public void eventDeserializerShouldParseEventWithHeaderAndBodyProjectName() {
    final String eventJson =
        "{\"projectName\":\"header_body_parser_project\",\"type\":\"project-created\","
            + " \"instanceId\":\"instance-id\"}";
    final Event event = deserializer.deserialize(eventJson);

    assertThat(event.instanceId).isEqualTo("instance-id");
  }

  @Test(expected = RuntimeException.class)
  public void eventDeserializerShouldFailForInvalidJson() {
    deserializer.deserialize("this is not a JSON string");
  }

  @Test(expected = RuntimeException.class)
  public void eventDeserializerShouldFailForInvalidObjectButValidJSON() {
    deserializer.deserialize("{}");
  }
}
