/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
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

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.calypsonet.terminal.calypso.crypto.legacysam.sam.LegacySam;
import org.calypsonet.terminal.card.CardSelectionResponseApi;
import org.calypsonet.terminal.card.spi.SmartCardSpi;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter of {@link LegacySam}.
 *
 * @since 0.1.0
 */
final class LegacySamAdapter implements LegacySam, SmartCardSpi {

  private static final Logger logger = LoggerFactory.getLogger(LegacySamAdapter.class);

  private final String powerOnData;
  private final ProductType samProductType;
  private final byte[] serialNumber;
  private final byte platform;
  private final byte applicationType;
  private final byte applicationSubType;
  private final byte softwareIssuer;
  private final byte softwareVersion;
  private final byte softwareRevision;
  private final byte classByte;
  private final SortedMap<Integer, Integer> eventCounters = new TreeMap<Integer, Integer>();
  private final SortedMap<Integer, Integer> eventCeilings = new TreeMap<Integer, Integer>();

  /**
   * Constructor.
   *
   * <p>Create the initial content from the data received in response to the card selection.
   *
   * @param cardSelectionResponse the response to the selection command.
   * @since 0.1.0
   */
  LegacySamAdapter(CardSelectionResponseApi cardSelectionResponse) {

    // in the case of a SAM, the power-on data corresponds to the ATR of the card.
    this.powerOnData = cardSelectionResponse.getPowerOnData();
    if (this.powerOnData == null) {
      throw new IllegalStateException("ATR should not be empty.");
    }

    serialNumber = new byte[4];

    /* extract the historical bytes from T3 to T12 */
    // CL-SAM-ATR.1
    String extractRegex = "3B(.{6}|.{10})805A(.{20})829000";
    Pattern pattern = Pattern.compile(extractRegex); // NOSONAR: hex strings here, regex is safe
    // to use
    Matcher matcher = pattern.matcher(powerOnData);
    if (matcher.find(0)) {
      byte[] atrSubElements = HexUtil.toByteArray(matcher.group(2));
      platform = atrSubElements[0];
      applicationType = atrSubElements[1];
      applicationSubType = atrSubElements[2];
      softwareIssuer = atrSubElements[3];
      softwareVersion = atrSubElements[4];
      softwareRevision = atrSubElements[5];

      // determine SAM product type from Application Subtype
      switch (applicationSubType) {
        case (byte) 0xC1:
          samProductType = softwareIssuer == (byte) 0x08 ? ProductType.HSM_C1 : ProductType.SAM_C1;
          break;
        case (byte) 0xD0:
        case (byte) 0xD1:
        case (byte) 0xD2:
          samProductType = ProductType.SAM_S1DX;
          break;
        case (byte) 0xE1:
          samProductType = ProductType.SAM_S1E1;
          break;
        default:
          samProductType = ProductType.UNKNOWN;
          break;
      }

      System.arraycopy(atrSubElements, 6, serialNumber, 0, 4);
      if (logger.isTraceEnabled()) {
        logger.trace(
            String.format(
                "SAM %s PLATFORM = %02Xh, APPTYPE = %02Xh, APPSUBTYPE = %02Xh, SWISSUER = %02Xh, SWVERSION = "
                    + "%02Xh, SWREVISION = %02Xh",
                samProductType.name(),
                platform,
                applicationType,
                applicationSubType,
                softwareIssuer,
                softwareVersion,
                softwareRevision));
        logger.trace("SAM SERIALNUMBER = {}", HexUtil.toHex(serialNumber));
      }
    } else {
      samProductType = ProductType.UNKNOWN;
      platform = 0;
      applicationType = 0;
      applicationSubType = 0;
      softwareIssuer = 0;
      softwareVersion = 0;
      softwareRevision = 0;
    }
    // CL-CLA-SAM.1
    classByte = samProductType == ProductType.SAM_S1DX ? (byte) 0x94 : (byte) 0x80;
  }

  /**
   * Gets the class byte to use for the current product type.
   *
   * @return A byte.
   * @since 0.1.0
   */
  byte getClassByte() {
    return classByte;
  }

  /**
   * Gets the maximum length allowed for digest commands.
   *
   * @return An positive int.
   * @since 0.1.0
   */
  int getMaxDigestDataLength() {
    switch (samProductType) {
      case SAM_C1:
      case HSM_C1:
        return 255;
      case SAM_S1DX:
        return 70;
      case SAM_S1E1:
        return 240;
      default:
        return 0;
    }
  }

  /**
   * {@inheritDoc}<br>
   * No select application for a SAM.
   *
   * @since 0.1.0
   */
  @Override
  public byte[] getSelectApplicationResponse() {
    return new byte[0];
  }

  /**
   * {@inheritDoc}
   *
   * @since 0.1.0
   */
  @Override
  public String getPowerOnData() {
    return powerOnData;
  }

  /**
   * {@inheritDoc}
   *
   * @since 0.1.0
   */
  @Override
  public ProductType getProductType() {
    return samProductType;
  }

  /**
   * Gets textual information about the SAM.
   *
   * @return A not empty String.
   */
  @Override
  public String getProductInfo() {
    return "Type: " + getProductType().name() + ", S/N: " + HexUtil.toHex(getSerialNumber());
  }

  /**
   * {@inheritDoc}
   *
   * @since 0.1.0
   */
  @Override
  public byte[] getSerialNumber() {
    return serialNumber;
  }

  /**
   * {@inheritDoc}
   *
   * @since 0.1.0
   */
  @Override
  public byte getPlatform() {
    return platform;
  }

  /**
   * {@inheritDoc}
   *
   * @since 0.1.0
   */
  @Override
  public byte getApplicationType() {
    return applicationType;
  }

  /**
   * {@inheritDoc}
   *
   * @since 0.1.0
   */
  @Override
  public byte getApplicationSubType() {
    return applicationSubType;
  }

  /**
   * {@inheritDoc}
   *
   * @since 0.1.0
   */
  @Override
  public byte getSoftwareIssuer() {
    return softwareIssuer;
  }

  /**
   * {@inheritDoc}
   *
   * @since 0.1.0
   */
  @Override
  public byte getSoftwareVersion() {
    return softwareVersion;
  }

  /**
   * {@inheritDoc}
   *
   * @since 0.1.0
   */
  @Override
  public byte getSoftwareRevision() {
    return softwareRevision;
  }

  /**
   * Adds or replace an event counter.
   *
   * @param eventCounterNumber The number of the counter.
   * @param eventCounterValue The counter value.
   * @since 0.1.0
   */
  void putEventCounter(int eventCounterNumber, int eventCounterValue) {
    this.eventCounters.put(eventCounterNumber, eventCounterValue);
  }

  /**
   * Adds or replace an event counter.
   *
   * @param eventCeilingNumber The number of the ceiling.
   * @param eventCeilingValue The ceiling value.
   * @since 0.1.0
   */
  void putEventCeiling(int eventCeilingNumber, int eventCeilingValue) {
    this.eventCeilings.put(eventCeilingNumber, eventCeilingValue);
  }

  /**
   * {@inheritDoc}
   *
   * @since 0.1.0
   */
  @Override
  public Integer getEventCounter(int eventCounterNumber) {
    return eventCounters.get(eventCounterNumber);
  }

  /**
   * {@inheritDoc}
   *
   * @since 0.1.0
   */
  @Override
  public SortedMap<Integer, Integer> getEventCounters() {
    return eventCounters;
  }

  /**
   * {@inheritDoc}
   *
   * @since 0.1.0
   */
  @Override
  public Integer getEventCeiling(int eventCeilingNumber) {
    return eventCeilings.get(eventCeilingNumber);
  }

  /**
   * {@inheritDoc}
   *
   * @since 0.1.0
   */
  @Override
  public SortedMap<Integer, Integer> getEventCeilings() {
    return eventCeilings;
  }

  /**
   * Gets the object content as a Json string.
   *
   * @return A not empty string.
   * @since 0.1.0
   */
  @Override
  public String toString() {
    return JsonUtil.toJson(this);
  }
}
