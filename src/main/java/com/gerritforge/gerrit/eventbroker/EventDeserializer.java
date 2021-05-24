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

import com.gerritforge.gerrit.eventbroker.EventMessage.Header;
import com.google.gerrit.server.events.Event;
import com.google.gson.Gson;
import com.google.inject.Inject;
import java.util.UUID;

public class EventDeserializer {

  private Gson gson;

  @Inject
  public EventDeserializer(Gson gson) {
    this.gson = gson;
  }

  public EventMessage deserialize(String json) {
    EventMessage result = gson.fromJson(json, EventMessage.class);
    if (result.getEvent() == null && result.getHeader() == null) {
      Event event = deserialiseEvent(json);
      result = new EventMessage(new Header(UUID.randomUUID(), event.instanceId), event);
    }
    result.validate();
    return result;
  }

  private Event deserialiseEvent(String json) {
    Event event = gson.fromJson(json, Event.class);
    requireNonNull(event.type, "Event type cannot be null");
    requireNonNull(event.instanceId, "Event instance id cannot be null");
    return event;
  }
}
