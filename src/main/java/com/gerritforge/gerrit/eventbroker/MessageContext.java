// Copyright (C) 2025 GerritForge, Inc.
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

/** Context for a message received from a broker. */
public interface MessageContext {

  /**
   * Commits the message offset.
   *
   * <p>If the broker is configured with auto-commit, this operation may be a no-op or a redundant
   * manual commit. If manual commit is enabled, this method MUST be called to acknowledge
   * successful processing.
   */
  void commit();
}
