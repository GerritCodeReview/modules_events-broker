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

import com.google.common.base.Strings;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventGson;
import com.google.gson.Gson;
import com.google.inject.Inject;

public class EventDeserializer {

  private Gson gson;

  @Inject
  public EventDeserializer(@EventGson Gson gson) {
    this.gson = gson;
  }

  public Event deserialize(String json) {
    Event resultEvent;
    EventMessage eventMessage = gson.fromJson(json, EventMessage.class);
    if (eventMessage.getEvent() == null && eventMessage.getHeader() == null) {
      resultEvent = gson.fromJson(json, Event.class);
    } else {
      eventMessage.validate();
      resultEvent = eventMessage.getEvent();
      if (Strings.isNullOrEmpty(resultEvent.instanceId)) {
        resultEvent.instanceId = eventMessage.getHeader().sourceInstanceId;
      }
    }
    validate(resultEvent);
    return resultEvent;
  }

  private void validate(Event event) {
    requireNonNull(event.type, "Event type cannot be null");
    requireNonNull(event.instanceId, "Event instance id cannot be null");
  }
}
