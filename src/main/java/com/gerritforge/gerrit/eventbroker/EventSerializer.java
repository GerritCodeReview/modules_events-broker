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

import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventGson;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;

/**
 * Serializes events, and their message metadata, into the JSON read by {@link EventDeserializer}.
 */
public class EventSerializer {

  private final Gson gson;

  @Inject
  public EventSerializer(@EventGson Gson gson) {
    this.gson = gson;
  }

  /**
   * Serializes an event into a JSON message.
   *
   * <p>Non-empty metadata is stored in the {@code meta} field of the event JSON object.
   *
   * @param event the event to serialize
   * @param metadata the metadata travelling with the event
   * @return the serialized message
   */
  public String serialize(Event event, EventMetadata metadata) {
    if (metadata != null && metadata.equals(EventMetadata.EMPTY)) {
      return gson.toJson(event);
    }
    JsonObject json = gson.toJsonTree(event).getAsJsonObject();
    json.add(EventMetadata.JSON_FIELD, gson.toJsonTree(metadata));
    return gson.toJson(json);
  }
}
