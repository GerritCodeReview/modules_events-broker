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

package com.gerritforge.gerrit.eventbroker;

import static java.util.Objects.requireNonNull;

import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventGson;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;

public class EventDeserializer {

  /**
   * An event together with the broker metadata travelling with it.
   *
   * @param event the deserialized event
   * @param metadata the message metadata, {@link EventMetadata#EMPTY} when absent
   */
  public record Message(Event event, EventMetadata metadata) {}

  private Gson gson;

  @Inject
  public EventDeserializer(@EventGson Gson gson) {
    this.gson = gson;
  }

  public Event deserialize(String json) {
    Event resultEvent = gson.fromJson(json, Event.class);

    validate(resultEvent);
    return resultEvent;
  }

  /**
   * Deserializes a message produced by {@link EventSerializer#serialize(Event, EventMetadata)}.
   *
   * @param json the serialized message
   * @return the event and its metadata
   */
  public Message deserializeMessage(String json) {
    JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
    Event event = gson.fromJson(jsonObject, Event.class);
    validate(event);

    EventMetadata metadata =
        gson.fromJson(jsonObject.get(EventMetadata.JSON_FIELD), EventMetadata.class);
    return new Message(event, metadata == null ? EventMetadata.EMPTY : metadata);
  }

  private void validate(Event event) {
    requireNonNull(event.type, "Event type cannot be null");
  }
}
