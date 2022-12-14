/* **************************************************************************************
 * Copyright (c) 2022 Calypso Networks Association https://calypsonet.org/
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

import org.calypsonet.terminal.calypso.crypto.legacysam.sam.LegacySam;
import org.calypsonet.terminal.calypso.crypto.legacysam.transaction.LSFreeTransactionManager;
import org.calypsonet.terminal.calypso.crypto.legacysam.transaction.LSTransactionManagerFactory;
import org.calypsonet.terminal.card.ProxyReaderApi;
import org.calypsonet.terminal.reader.CardReader;

/**
 * Adapter of {@link LSTransactionManagerFactory}.
 *
 * @since 0.1.0
 */
class LSTransactionManagerFactoryAdapter implements LSTransactionManagerFactory {

  /**
   * {@inheritDoc}
   *
   * @since 0.1.0
   */
  @Override
  public LSFreeTransactionManager createFreeTransactionManager(
      CardReader samReader, LegacySam sam) {
    if (!(samReader instanceof ProxyReaderApi)) {
      throw new IllegalArgumentException(
          "The provided 'samReader' must implement 'ProxyReaderApi'");
    }
    if (!(sam instanceof LegacySamAdapter)) {
      throw new IllegalArgumentException(
          "The provided 'sam' must be an instance of 'LegacySamAdapter'");
    }
    return new LSFreeTransactionManagerAdapter((ProxyReaderApi) samReader, (LegacySamAdapter) sam);
  }
}
