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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.lib.Config;

@Singleton
public class EventsBrokerConfiguration {
  static final String EVENTS_BROKER_FILE_NAME = "events-broker";
  static final String PARTITION_EVENT_PROPERTY_FIELD = "partitionEventProperty";
  static final String PARTITION_VALUE_FIELD = "partitionValue";
  static final String TOPIC_SECTION = "topic";

  static final String DEFAULT_PARTITION_EVENT_PROPERTY = "type";

  private final ImmutableMap<String, ImmutableList<String>> topicToPartitions;
  private final ImmutableMap<String, String> topicToEventProperty;

  @Inject
  public EventsBrokerConfiguration(PluginConfigFactory configFactory) {
    // This class is reused by broker implementations, so @PluginName would resolve to the
    // implementation plugin name (for example events-kafka) instead of the shared broker config.
    Config config = configFactory.getGlobalPluginConfig(EVENTS_BROKER_FILE_NAME);
    ImmutableMap.Builder<String, ImmutableList<String>> partitionsByTopic = ImmutableMap.builder();
    ImmutableMap.Builder<String, String> eventPropertyByTopic = ImmutableMap.builder();

    for (String subsection : config.getSubsections(TOPIC_SECTION)) {
      ImmutableList<String> partitionValue =
          ImmutableList.copyOf(
              config.getStringList(TOPIC_SECTION, subsection, PARTITION_VALUE_FIELD));
      if (partitionValue.size() != ImmutableSet.copyOf(partitionValue).size()) {
        throw new IllegalArgumentException(
            String.format("Duplicate partitionValue entries configured for topic %s", subsection));
      }

      partitionsByTopic.put(subsection, partitionValue);
      eventPropertyByTopic.put(
          subsection,
          Optional.ofNullable(
                  config.getString(TOPIC_SECTION, subsection, PARTITION_EVENT_PROPERTY_FIELD))
              .orElse(DEFAULT_PARTITION_EVENT_PROPERTY));
    }
    topicToPartitions = partitionsByTopic.build();
    topicToEventProperty = eventPropertyByTopic.build();
  }

  /**
   * Returns the configured logical partition values for the topic.
   *
   * <p>Returns an empty list when the topic has no configured partition values.
   */
  public List<String> getPartitionsForTopic(String topic) {
    return topicToPartitions.getOrDefault(topic, ImmutableList.of());
  }

  /**
   * Returns the event property used to pick a logical partition for the topic.
   *
   * <p>Returns {@link Optional#empty()} when the topic has no partition metadata.
   */
  public Optional<String> getEventPropertyForTopic(String topic) {
    return Optional.ofNullable(topicToEventProperty.get(topic));
  }
}
