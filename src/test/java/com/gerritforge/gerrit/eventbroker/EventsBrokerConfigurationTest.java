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
import static org.mockito.Mockito.when;

import com.google.gerrit.server.config.PluginConfigFactory;
import java.util.Arrays;
import org.eclipse.jgit.lib.Config;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EventsBrokerConfigurationTest {
  private static final String PLUGIN_NAME = "events-broker";
  private static final String TOPIC = "stream-events";

  @Mock private PluginConfigFactory configFactory;

  @Test
  public void shouldReturnConfiguredPartitionsForTopic() {
    Config config = new Config();
    config.setStringList(
        EventsBrokerConfiguration.TOPIC_SECTION,
        TOPIC,
        EventsBrokerConfiguration.PARTITION_VALUES_FIELD,
        Arrays.asList("change-index", "account-index"));

    EventsBrokerConfiguration configuration = configuration(config);

    assertThat(configuration.getPartitionsForTopic(TOPIC))
        .containsExactly("change-index", "account-index")
        .inOrder();
  }

  @Test
  public void shouldReturnConfiguredEventPropertyForTopic() {
    Config config = new Config();
    config.setString(
        EventsBrokerConfiguration.TOPIC_SECTION,
        TOPIC,
        EventsBrokerConfiguration.PARTITION_EVENT_PROPERTY_FIELD,
        "eventType");

    EventsBrokerConfiguration configuration = configuration(config);

    assertThat(configuration.getEventPropertyForTopic(TOPIC)).isEqualTo("eventType");
  }

  @Test
  public void shouldReturnDefaultEventPropertyWhenNotConfiguredForTopic() {
    Config config = new Config();
    config.setStringList(
        EventsBrokerConfiguration.TOPIC_SECTION,
        TOPIC,
        EventsBrokerConfiguration.PARTITION_VALUES_FIELD,
        Arrays.asList("change-index"));

    EventsBrokerConfiguration configuration = configuration(config);

    assertThat(configuration.getEventPropertyForTopic(TOPIC))
        .isEqualTo(EventsBrokerConfiguration.DEFAULT_PARTITION_EVENT_PROPERTY);
  }

  @Test
  public void shouldReturnEmptyPartitionsWhenTopicHasNoPartitionValues() {
    Config config = new Config();
    config.setString(
        EventsBrokerConfiguration.TOPIC_SECTION,
        TOPIC,
        EventsBrokerConfiguration.PARTITION_EVENT_PROPERTY_FIELD,
        "eventType");

    EventsBrokerConfiguration configuration = configuration(config);

    assertThat(configuration.getPartitionsForTopic(TOPIC)).isEmpty();
  }

  @Test
  public void shouldReturnNullForUnknownTopic() {
    EventsBrokerConfiguration configuration = configuration(new Config());

    assertThat(configuration.getPartitionsForTopic(TOPIC)).isNull();
    assertThat(configuration.getEventPropertyForTopic(TOPIC)).isNull();
  }

  private EventsBrokerConfiguration configuration(Config config) {
    when(configFactory.getGlobalPluginConfig(PLUGIN_NAME)).thenReturn(config);
    return new EventsBrokerConfiguration(configFactory, PLUGIN_NAME);
  }
}
