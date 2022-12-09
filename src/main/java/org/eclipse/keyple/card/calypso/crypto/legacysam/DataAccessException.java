/* **************************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.card.calypso.crypto.legacysam;

/**
 * Indicates that the content of the command is incompatible with the SAM's file system (e.g.
 * signing key not found,...).
 *
 * @since 0.1.0
 */
final class DataAccessException extends CommandException {

  /**
   * @param message the message to identify the exception context.
   * @since 0.1.0
   */
  DataAccessException(String message) {
    super(message);
  }
}
