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

import static com.google.common.truth.Truth.assertThat;

import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventGsonProvider;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

public class EventSerializerTest {
  private Gson gson;
  private EventSerializer serializer;
  private EventDeserializer deserializer;
  private ProjectCreatedEvent event;

  @Before
  public void setUp() {
    gson = new EventGsonProvider().get();
    serializer = new EventSerializer(gson);
    deserializer = new EventDeserializer(gson);

    event = new ProjectCreatedEvent();
    event.instanceId = "instance-id";
    event.projectName = "project";
    event.headName = "refs/heads/master";
  }

  @Test
  public void serializeWithEmptyMetadataShouldRoundtripWithLegacyDeserialize() {
    String serialized = serializer.serialize(event, EventMetadata.EMPTY);

    assertThat(serialized).isEqualTo(gson.toJson(event));
    Event deserialized = new EventDeserializer(gson).deserialize(serialized);
    assertThat(deserialized).isInstanceOf(ProjectCreatedEvent.class);
    ProjectCreatedEvent deserializedEvent = (ProjectCreatedEvent) deserialized;
    assertThat(deserializedEvent.instanceId).isEqualTo(event.instanceId);
    assertThat(deserializedEvent.projectName).isEqualTo(event.projectName);
    assertThat(deserializedEvent.headName).isEqualTo(event.headName);
  }

  @Test
  public void messageWithMetadataShouldRoundtripWithLegacyDeserialize() {
    EventMetadata metadata =
        EventMetadata.EMPTY.nextRetry("first-instance").nextRetry("second-instance");
    String serialized = serializer.serialize(event, metadata);

    Event message = deserializer.deserialize(serialized);
    ProjectCreatedEvent deserialized = (ProjectCreatedEvent) message;

    assertThat(deserialized.projectName).isEqualTo(event.projectName);
    assertThat(deserialized.headName).isEqualTo(event.headName);
    assertThat(deserialized.instanceId).isEqualTo(event.instanceId);
  }

  @Test
  public void messageWithEmptyMetadataShouldRoundtrip() {
    String serialized = serializer.serialize(event, EventMetadata.EMPTY);

    EventDeserializer.Message message = deserializer.deserializeMessage(serialized);

    assertThat(message.event()).isInstanceOf(event.getClass());
    assertThat(message.metadata()).isEqualTo(EventMetadata.EMPTY);
  }

  @Test
  public void messageWithMetadataShouldRoundtrip() {
    EventMetadata metadata =
        EventMetadata.EMPTY.nextRetry("first-instance").nextRetry("second-instance");
    String serialized = serializer.serialize(event, metadata);

    EventDeserializer.Message message = deserializer.deserializeMessage(serialized);
    ProjectCreatedEvent deserialized = (ProjectCreatedEvent) message.event();

    assertThat(message.metadata()).isEqualTo(metadata);
    assertThat(deserialized.projectName).isEqualTo(event.projectName);
    assertThat(deserialized.headName).isEqualTo(event.headName);
    assertThat(deserialized.instanceId).isEqualTo(event.instanceId);
  }
}
