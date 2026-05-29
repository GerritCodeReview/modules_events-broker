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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.jgit.lib.Config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class EventsBrokerConfiguration {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static final String PARTITION_EVENT_PROPERTY_FIELD = "partitionEventProperty";
  static final String PARTITION_VALUES_FIELD = "partitionValues";
  static final String TOPIC_SECTION = "topic";

  static final String DEFAULT_PARTITION_EVENT_PROPERTY = "type";

  private final Map<String, List<String>> topicToPartitions = new HashMap<>();
  private final Map<String, String> topicToEventProperty = new HashMap<>();

  @Inject
  public EventsBrokerConfiguration(PluginConfigFactory configFactory, @PluginName String pluginName) {
    Config config = configFactory.getGlobalPluginConfig(pluginName);

    for (String subsection : config.getSubsections(TOPIC_SECTION)) {
      List<String> partitionValues =
          Arrays.asList(config.getStringList(
              TOPIC_SECTION,
              subsection,
              PARTITION_VALUES_FIELD));

      topicToPartitions.put(subsection, partitionValues);
      topicToEventProperty.put(subsection, Optional.ofNullable(
              config.getString(TOPIC_SECTION, subsection, PARTITION_EVENT_PROPERTY_FIELD)).orElse(DEFAULT_PARTITION_EVENT_PROPERTY));
    }
  }

  public List<String> getPartitionsForTopic(String topic) {
    return topicToPartitions.get(topic);
  }

  public String getEventPropertyForTopic(String topic) {
    return topicToEventProperty.get(topic);
  }
}
