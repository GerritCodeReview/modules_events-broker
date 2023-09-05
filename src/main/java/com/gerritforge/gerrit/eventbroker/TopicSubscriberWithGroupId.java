package com.gerritforge.gerrit.eventbroker;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class TopicSubscriberWithGroupId {
  public static TopicSubscriberWithGroupId topicSubscriberWithGroupId(
      String groupId, TopicSubscriber topicSubscriber) {
    return new AutoValue_TopicSubscriberWithGroupId(groupId, topicSubscriber);
  }

  public abstract String groupId();

  public abstract TopicSubscriber topicSubscriber();
}
