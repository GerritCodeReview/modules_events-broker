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
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jgit.lib.Config;

@Singleton
public class EventsBrokerConfiguration {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static final String EVENTS_BROKER_FILE_NAME = "events-broker";
  static final String PARTITION_EVENT_PROPERTY_FIELD = "partitionEventProperty";
  static final String PARTITION_VALUE_FIELD = "partitionValue";
  static final String TOPIC_SECTION = "topic";

  static final String DEFAULT_PARTITION_EVENT_PROPERTY = "type";

  private final Map<String, List<String>> topicToPartitions = new HashMap<>();
  private final Map<String, String> topicToEventProperty = new HashMap<>();

  @Inject
  public EventsBrokerConfiguration(PluginConfigFactory configFactory) {
    // This class is reused by broker implementations, so @PluginName would resolve to the
    // implementation plugin name (for example events-kafka) instead of the shared broker config.
    Config config = configFactory.getGlobalPluginConfig(EVENTS_BROKER_FILE_NAME);

    for (String subsection : config.getSubsections(TOPIC_SECTION)) {
      List<String> partitionValue =
          ImmutableList.copyOf(
              config.getStringList(TOPIC_SECTION, subsection, PARTITION_VALUE_FIELD));

      topicToPartitions.put(subsection, partitionValue);
      topicToEventProperty.put(
          subsection,
          Optional.ofNullable(
                  config.getString(TOPIC_SECTION, subsection, PARTITION_EVENT_PROPERTY_FIELD))
              .orElse(DEFAULT_PARTITION_EVENT_PROPERTY));
    }
  }

  /**
   * Returns the configured logical partition values for the topic.
   *
   * <p>Returns {@code null} when the topic has no partition metadata. Returns an empty list when
   * the topic is configured but has no partition values.
   */
  public List<String> getPartitionsForTopic(String topic) {
    return topicToPartitions.get(topic);
  }

  /**
   * Returns the event property used to pick a logical partition for the topic.
   *
   * <p>Returns {@code null} when the topic has no partition metadata.
   */
  public String getEventPropertyForTopic(String topic) {
    return topicToEventProperty.get(topic);
  }
}
