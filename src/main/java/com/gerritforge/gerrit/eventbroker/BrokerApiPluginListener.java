package com.gerritforge.gerrit.eventbroker;

import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.plugins.Plugin;

public interface BrokerApiPluginListener {

  /** Returns the item a broker plugin binds its {@link BrokerApi} into. */
  DynamicItem<BrokerApi> brokerApiDynamicItem();

  /**
   * Returns true if the plugin exposes a {@link DynamicItem} of {@link BrokerApi}.
   *
   * @param plugin plugin to check
   * @return true if plugin exposes a {@link DynamicItem} of {@link BrokerApi}
   */
  default boolean isBrokerApiPlugin(Plugin plugin) {
    return plugin.getName().equals(brokerApiDynamicItem().getPluginName());
  }
}
