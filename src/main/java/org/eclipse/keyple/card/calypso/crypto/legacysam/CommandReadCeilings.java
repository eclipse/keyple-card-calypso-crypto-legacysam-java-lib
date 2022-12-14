/* **************************************************************************************
 * Copyright (c) 2019 Calypso Networks Association https://calypsonet.org/
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

import static org.eclipse.keyple.card.calypso.crypto.legacysam.DtoAdapters.*;

import java.util.*;
import org.calypsonet.terminal.card.ApduResponseApi;
import org.eclipse.keyple.core.util.ApduUtil;
import org.eclipse.keyple.core.util.ByteArrayUtil;

/**
 * Builds the Read Ceilings APDU command.
 *
 * @since 0.1.0
 */
final class CommandReadCeilings extends Command {

  /** Ceiling operation type */
  enum CeilingsOperationType {
    /** Single ceiling */
    READ_SINGLE_CEILING,
    /** Ceiling record */
    READ_CEILING_RECORD
  }

  private final CeilingsOperationType ceilingsOperationType;
  private final int firstEventCeilingNumber;

  private static final Map<Integer, StatusProperties> STATUS_TABLE;

  static {
    Map<Integer, StatusProperties> m = new HashMap<Integer, StatusProperties>(Command.STATUS_TABLE);
    m.put(
        0x6900,
        new StatusProperties(
            "An event counter cannot be incremented.", CounterOverflowException.class));
    m.put(0x6A00, new StatusProperties("Incorrect P1 or P2.", IllegalParameterException.class));
    m.put(0x6200, new StatusProperties("Correct execution with warning: data not signed."));
    STATUS_TABLE = m;
  }

  /**
   * Instantiates a new CmdSamReadCeilings.
   *
   * @param legacySam The Calypso legacy SAM.
   * @param ceilingsOperationType the ceiling operation type.
   * @param target the ceiling index (0-26) if READ_SINGLE_CEILING, the record index (1-3) if
   *     READ_CEILING_RECORD.
   * @since 0.1.0
   */
  CommandReadCeilings(
      LegacySamAdapter legacySam, CeilingsOperationType ceilingsOperationType, int target) {

    super(CommandRef.READ_CEILINGS, 48, legacySam);

    byte cla = legacySam.getClassByte();

    byte p1;
    byte p2;
    this.ceilingsOperationType = ceilingsOperationType;
    if (ceilingsOperationType == CeilingsOperationType.READ_SINGLE_CEILING) {
      this.firstEventCeilingNumber = target;
      p1 = (byte) target;
      p2 = (byte) (0xB8);
    } else {
      this.firstEventCeilingNumber = (target - 1) * 9;
      p1 = (byte) 0x00;
      p2 = (byte) (0xB0 + target);
    }

    setApduRequest(
        new ApduRequestAdapter(
            ApduUtil.build(cla, getCommandRef().getInstructionByte(), p1, p2, null, (byte) 0x00)));
  }

  /**
   * {@inheritDoc}
   *
   * @since 0.1.0
   */
  @Override
  Map<Integer, StatusProperties> getStatusTable() {
    return STATUS_TABLE;
  }

  /**
   * {@inheritDoc}
   *
   * @since 0.1.0
   */
  @Override
  void parseApduResponse(ApduResponseApi apduResponse) throws CommandException {
    super.parseApduResponse(apduResponse);
    byte[] dataOut = apduResponse.getDataOut();
    if (ceilingsOperationType == CeilingsOperationType.READ_SINGLE_CEILING) {
      getLegacySam().putEventCeiling(dataOut[8], ByteArrayUtil.extractInt(dataOut, 9, 3, false));
    } else {
      for (int i = 0; i < 9; i++) {
        getLegacySam()
            .putEventCeiling(
                firstEventCeilingNumber + i,
                ByteArrayUtil.extractInt(dataOut, 8 + (3 * i), 3, false));
      }
    }
  }
}
