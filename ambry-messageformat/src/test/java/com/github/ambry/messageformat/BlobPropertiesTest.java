/**
 * Copyright 2016 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package com.github.ambry.messageformat;

import com.github.ambry.clustermap.MockPartitionId;
import com.github.ambry.commons.BlobId;
import com.github.ambry.utils.ByteBufferInputStream;
import com.github.ambry.utils.SystemTime;
import com.github.ambry.utils.TestUtils;
import com.github.ambry.utils.Utils;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static com.github.ambry.account.Account.*;
import static com.github.ambry.account.Container.*;
import static com.github.ambry.messageformat.BlobPropertiesSerDe.*;
import static com.github.ambry.messageformat.MessageFormatRecord.*;
import static org.junit.Assert.*;


/**
 * Basic tests for BlobProperties
 */
@RunWith(Parameterized.class)
public class BlobPropertiesTest {

  private final short version;
  //will remove this part once serialize logic is in.
  private static final int VERSION_FIELD_SIZE_IN_BYTES = Short.BYTES;
  private static final int TTL_FIELD_SIZE_IN_BYTES = Long.BYTES;
  private static final int PRIVATE_FIELD_SIZE_IN_BYTES = Byte.BYTES;
  private static final int CREATION_TIME_FIELD_SIZE_IN_BYTES = Long.BYTES;
  private static final int BLOB_SIZE_FIELD_SIZE_IN_BYTES = Long.BYTES;
  private static final int ENCRYPTED_FIELD_SIZE_IN_BYTES = Byte.BYTES;

  /**
   * Running for {@link BlobPropertiesSerDe#VERSION_1} and {@link BlobPropertiesSerDe#VERSION_2}
   * @return an array with both the versions ({@link BlobPropertiesSerDe#VERSION_1} and {@link BlobPropertiesSerDe#VERSION_2}).
   */
  @Parameterized.Parameters
  public static List<Object[]> data() {
    return Arrays.asList(new Object[][]{{BlobPropertiesSerDe.VERSION_1}, {BlobPropertiesSerDe.VERSION_2},
        {BlobPropertiesSerDe.VERSION_3}, {BlobPropertiesSerDe.VERSION_4}});
  }

  public BlobPropertiesTest(short version) {
    this.version = version;
  }

  @Test
  public void basicTest() throws IOException {
    int blobSize = 100;
    String serviceId = "ServiceId";
    String ownerId = "OwnerId";
    String contentType = "ContentType";
    String externalAssetTag = "some-external-asset-tag";
    String contentEncoding = version == BlobPropertiesSerDe.VERSION_4 ? "gzip" : null;
    String filename = version == BlobPropertiesSerDe.VERSION_4 ? "filename" : null;
    int timeToLiveInSeconds = 144;
    short accountId = Utils.getRandomShort(TestUtils.RANDOM);
    short containerId = Utils.getRandomShort(TestUtils.RANDOM);
    boolean isEncrypted = TestUtils.RANDOM.nextBoolean();

    short accountIdToExpect = version == BlobPropertiesSerDe.VERSION_1 ? UNKNOWN_ACCOUNT_ID : accountId;
    short containerIdToExpect = version == BlobPropertiesSerDe.VERSION_1 ? UNKNOWN_CONTAINER_ID : containerId;
    boolean encryptFlagToExpect = version >= BlobPropertiesSerDe.VERSION_3 && isEncrypted;
    String contentEncodingToExpect = contentEncoding;
    String filenameToExpect = filename;
    String blobId = new BlobId(BlobId.BLOB_ID_V6, BlobId.BlobIdType.NATIVE, (byte) 1, accountId, containerId,
        new MockPartitionId(), false, BlobId.BlobDataType.METADATA).getID();

    BlobProperties blobProperties = new BlobProperties(blobSize, serviceId, null, null, false, Utils.Infinite_Time,
        SystemTime.getInstance().milliseconds(), accountId, containerId, isEncrypted, externalAssetTag, null, null, null);
    System.out.println(blobProperties.toString()); // Provide example of BlobProperties.toString()
    ByteBuffer serializedBuffer = serializeBlobPropertiesInVersion(blobProperties);
    blobProperties = BlobPropertiesSerDe.getBlobPropertiesFromStream(
        new DataInputStream(new ByteBufferInputStream(serializedBuffer)));
    verifyBlobProperties(blobProperties, blobSize, serviceId, "", "", false, Utils.Infinite_Time, accountIdToExpect,
        containerIdToExpect, encryptFlagToExpect, null, null, null, null);

    blobProperties = new BlobProperties(blobSize, serviceId, null, null, false, Utils.Infinite_Time,
        SystemTime.getInstance().milliseconds(), accountId, containerId, isEncrypted, externalAssetTag, contentEncoding,
        filename, null);
    System.out.println(blobProperties.toString()); // Provide example of BlobProperties.toString()
    serializedBuffer = serializeBlobPropertiesInVersion(blobProperties);
    blobProperties = BlobPropertiesSerDe.getBlobPropertiesFromStream(
        new DataInputStream(new ByteBufferInputStream(serializedBuffer)));
    verifyBlobProperties(blobProperties, blobSize, serviceId, "", "", false, Utils.Infinite_Time, accountIdToExpect,
        containerIdToExpect, encryptFlagToExpect, null, contentEncodingToExpect, filenameToExpect, null);
    assertTrue(blobProperties.getCreationTimeInMs() > 0);
    assertTrue(blobProperties.getCreationTimeInMs() <= System.currentTimeMillis());

    blobProperties = new BlobProperties(blobSize, serviceId, null, null, false, Utils.Infinite_Time,
        SystemTime.getInstance().milliseconds(), accountId, containerId, isEncrypted, externalAssetTag, contentEncoding,
        filename, null);
    serializedBuffer = serializeBlobPropertiesInVersion(blobProperties);
    blobProperties = BlobPropertiesSerDe.getBlobPropertiesFromStream(
        new DataInputStream(new ByteBufferInputStream(serializedBuffer)));
    verifyBlobProperties(blobProperties, blobSize, serviceId, "", "", false, Utils.Infinite_Time, accountIdToExpect,
        containerIdToExpect, encryptFlagToExpect, null, contentEncodingToExpect, filenameToExpect, null);

    blobProperties =
        new BlobProperties(blobSize, serviceId, ownerId, contentType, true, timeToLiveInSeconds, accountId, containerId,
            isEncrypted, externalAssetTag, contentEncoding, filename);
    System.out.println(blobProperties.toString()); // Provide example of BlobProperties.toString()

    serializedBuffer = serializeBlobPropertiesInVersion(blobProperties);
    blobProperties = BlobPropertiesSerDe.getBlobPropertiesFromStream(
        new DataInputStream(new ByteBufferInputStream(serializedBuffer)));
    verifyBlobProperties(blobProperties, blobSize, serviceId, ownerId, contentType, true, timeToLiveInSeconds,
        accountIdToExpect, containerIdToExpect, encryptFlagToExpect, null, contentEncodingToExpect, filenameToExpect, null);
    assertTrue(blobProperties.getCreationTimeInMs() > 0);
    assertTrue(blobProperties.getCreationTimeInMs() <= System.currentTimeMillis());

    long creationTimeMs = SystemTime.getInstance().milliseconds();
    blobProperties =
        new BlobProperties(blobSize, serviceId, ownerId, contentType, true, timeToLiveInSeconds, creationTimeMs,
            accountId, containerId, isEncrypted, "some-external-asset-tag", contentEncoding, filename, null);
    System.out.println(blobProperties.toString()); // Provide example of BlobProperties.toString()
    serializedBuffer = serializeBlobPropertiesInVersion(blobProperties);

    blobProperties = BlobPropertiesSerDe.getBlobPropertiesFromStream(
        new DataInputStream(new ByteBufferInputStream(serializedBuffer)));
    verifyBlobProperties(blobProperties, blobSize, serviceId, ownerId, contentType, true, timeToLiveInSeconds,
        accountIdToExpect, containerIdToExpect, encryptFlagToExpect, null, contentEncodingToExpect, filenameToExpect, null);
    assertEquals(blobProperties.getCreationTimeInMs(), creationTimeMs);

    long creationTimeInSecs = TimeUnit.MILLISECONDS.toSeconds(creationTimeMs);
    // valid TTLs
    long[] validTTLs =
        new long[]{TimeUnit.HOURS.toSeconds(1), TimeUnit.HOURS.toSeconds(10), TimeUnit.HOURS.toSeconds(100),
            TimeUnit.DAYS.toSeconds(1), TimeUnit.DAYS.toSeconds(10), TimeUnit.DAYS.toSeconds(100),
            TimeUnit.DAYS.toSeconds(30 * 12), TimeUnit.DAYS.toSeconds(30 * 12 * 10),
            Integer.MAX_VALUE - creationTimeInSecs - 1, Integer.MAX_VALUE - creationTimeInSecs,
            Integer.MAX_VALUE - creationTimeInSecs + 1, Integer.MAX_VALUE - creationTimeInSecs + 100,
            Integer.MAX_VALUE - creationTimeInSecs + 10000};
    for (long ttl : validTTLs) {
      blobProperties =
          new BlobProperties(blobSize, serviceId, ownerId, contentType, true, ttl, creationTimeMs, accountId,
              containerId, isEncrypted, null, contentEncoding, filename, null);
      serializedBuffer = serializeBlobPropertiesInVersion(blobProperties);
      blobProperties = BlobPropertiesSerDe.getBlobPropertiesFromStream(
          new DataInputStream(new ByteBufferInputStream(serializedBuffer)));
      verifyBlobProperties(blobProperties, blobSize, serviceId, ownerId, contentType, true, ttl, accountIdToExpect,
          containerIdToExpect, encryptFlagToExpect, null, contentEncodingToExpect, filenameToExpect, null);
    }

    blobProperties =
        new BlobProperties(blobSize, serviceId, null, null, false, timeToLiveInSeconds, creationTimeMs, accountId,
            containerId, isEncrypted, externalAssetTag, contentEncoding, filename, null);
    verifyBlobProperties(blobProperties, blobSize, serviceId, null, null, false, timeToLiveInSeconds, accountId,
        containerId, isEncrypted, externalAssetTag, contentEncodingToExpect, filenameToExpect, null);
    blobProperties.setTimeToLiveInSeconds(timeToLiveInSeconds + 1);
    verifyBlobProperties(blobProperties, blobSize, serviceId, null, null, false, timeToLiveInSeconds + 1, accountId,
        containerId, isEncrypted, externalAssetTag, contentEncodingToExpect, filenameToExpect, null);
    serializedBuffer = serializeBlobPropertiesInVersion(blobProperties);
    blobProperties = BlobPropertiesSerDe.getBlobPropertiesFromStream(
        new DataInputStream(new ByteBufferInputStream(serializedBuffer)));
    verifyBlobProperties(blobProperties, blobSize, serviceId, "", "", false, timeToLiveInSeconds + 1, accountIdToExpect,
        containerIdToExpect, encryptFlagToExpect, null, contentEncodingToExpect, filenameToExpect, null);

    blobProperties.setBlobSize(blobSize + 1);
    verifyBlobProperties(blobProperties, blobSize + 1, serviceId, "", "", false, timeToLiveInSeconds + 1,
        accountIdToExpect, containerIdToExpect, encryptFlagToExpect, null, contentEncodingToExpect, filenameToExpect, null);
    serializedBuffer = serializeBlobPropertiesInVersion(blobProperties);
    blobProperties = BlobPropertiesSerDe.getBlobPropertiesFromStream(
        new DataInputStream(new ByteBufferInputStream(serializedBuffer)));
    verifyBlobProperties(blobProperties, blobSize + 1, serviceId, "", "", false, timeToLiveInSeconds + 1,
        accountIdToExpect, containerIdToExpect, encryptFlagToExpect, null, contentEncodingToExpect, filenameToExpect, null);

    blobProperties =
        new BlobProperties(blobSize, serviceId, null, null, false, timeToLiveInSeconds,
            creationTimeMs, accountId, containerId, isEncrypted, externalAssetTag, contentEncoding, filename, blobId);
    verifyBlobProperties(blobProperties, blobSize, serviceId, null, null, false, timeToLiveInSeconds, accountId,
        containerId, isEncrypted, externalAssetTag, contentEncodingToExpect, filenameToExpect, blobId);
    blobProperties.setTimeToLiveInSeconds(timeToLiveInSeconds + 1);
    verifyBlobProperties(blobProperties, blobSize, serviceId, null, null, false, timeToLiveInSeconds + 1, accountId,
        containerId, isEncrypted, externalAssetTag, contentEncodingToExpect, filenameToExpect, blobId);
    serializedBuffer = serializeBlobPropertiesInVersion(blobProperties);
    blobProperties = BlobPropertiesSerDe.getBlobPropertiesFromStream(
        new DataInputStream(new ByteBufferInputStream(serializedBuffer)));
    verifyBlobProperties(blobProperties, blobSize, serviceId, "", "", false, timeToLiveInSeconds + 1, accountIdToExpect,
        containerIdToExpect, encryptFlagToExpect, null, contentEncodingToExpect, filenameToExpect, null);
  }

  /**
   * Serialize {@link BlobProperties} using {@link BlobPropertiesSerDe} in the given version
   * @param blobProperties {@link BlobProperties} that needs to be serialized
   * @return the {@link ByteBuffer} containing the serialized {@link BlobPropertiesSerDe}
   */
  private ByteBuffer serializeBlobPropertiesInVersion(BlobProperties blobProperties) {
    ByteBuffer outputBuffer = null;
    switch (version) {
      case BlobPropertiesSerDe.VERSION_1:
        int size = Version_Field_Size_In_Bytes + Long.BYTES + Byte.BYTES + Long.BYTES + Long.BYTES + Integer.BYTES
            + Utils.getNullableStringLength(blobProperties.getContentType()) + Integer.BYTES
            + Utils.getNullableStringLength(blobProperties.getOwnerId()) + Integer.BYTES
            + Utils.getNullableStringLength(blobProperties.getServiceId());
        outputBuffer = ByteBuffer.allocate(size);
        outputBuffer.putShort(VERSION_1);
        outputBuffer.putLong(blobProperties.getTimeToLiveInSeconds());
        outputBuffer.put(blobProperties.isPrivate() ? (byte) 1 : (byte) 0);
        outputBuffer.putLong(blobProperties.getCreationTimeInMs());
        outputBuffer.putLong(blobProperties.getBlobSize());
        Utils.serializeNullableString(outputBuffer, blobProperties.getContentType());
        Utils.serializeNullableString(outputBuffer, blobProperties.getOwnerId());
        Utils.serializeNullableString(outputBuffer, blobProperties.getServiceId());
        outputBuffer.flip();
        break;
      case BlobPropertiesSerDe.VERSION_2:
        size = Version_Field_Size_In_Bytes + Long.BYTES + Byte.BYTES + Long.BYTES + Long.BYTES + Integer.BYTES
            + Utils.getNullableStringLength(blobProperties.getContentType()) + Integer.BYTES
            + Utils.getNullableStringLength(blobProperties.getOwnerId()) + Integer.BYTES
            + Utils.getNullableStringLength(blobProperties.getServiceId()) + Short.BYTES + Short.BYTES;
        outputBuffer = ByteBuffer.allocate(size);
        outputBuffer.putShort(VERSION_2);
        outputBuffer.putLong(blobProperties.getTimeToLiveInSeconds());
        outputBuffer.put(blobProperties.isPrivate() ? (byte) 1 : (byte) 0);
        outputBuffer.putLong(blobProperties.getCreationTimeInMs());
        outputBuffer.putLong(blobProperties.getBlobSize());
        Utils.serializeNullableString(outputBuffer, blobProperties.getContentType());
        Utils.serializeNullableString(outputBuffer, blobProperties.getOwnerId());
        Utils.serializeNullableString(outputBuffer, blobProperties.getServiceId());
        outputBuffer.putShort(blobProperties.getAccountId());
        outputBuffer.putShort(blobProperties.getContainerId());
        outputBuffer.flip();
        break;
      case BlobPropertiesSerDe.VERSION_3:
        size = VERSION_FIELD_SIZE_IN_BYTES + TTL_FIELD_SIZE_IN_BYTES + PRIVATE_FIELD_SIZE_IN_BYTES
            + CREATION_TIME_FIELD_SIZE_IN_BYTES + BLOB_SIZE_FIELD_SIZE_IN_BYTES + Utils.getIntStringLength(
            blobProperties.getContentType()) + Utils.getIntStringLength(blobProperties.getOwnerId())
            + Utils.getIntStringLength(blobProperties.getServiceId()) + Short.BYTES + Short.BYTES
            + ENCRYPTED_FIELD_SIZE_IN_BYTES;
        outputBuffer = ByteBuffer.allocate(size);
        outputBuffer.putShort(VERSION_3);
        outputBuffer.putLong(blobProperties.getTimeToLiveInSeconds());
        outputBuffer.put(blobProperties.isPrivate() ? (byte) 1 : (byte) 0);
        outputBuffer.putLong(blobProperties.getCreationTimeInMs());
        outputBuffer.putLong(blobProperties.getBlobSize());
        Utils.serializeNullableString(outputBuffer, blobProperties.getContentType());
        Utils.serializeNullableString(outputBuffer, blobProperties.getOwnerId());
        Utils.serializeNullableString(outputBuffer, blobProperties.getServiceId());
        outputBuffer.putShort(blobProperties.getAccountId());
        outputBuffer.putShort(blobProperties.getContainerId());
        outputBuffer.put(blobProperties.isEncrypted() ? (byte) 1 : (byte) 0);
        outputBuffer.flip();
        break;
      case BlobPropertiesSerDe.VERSION_4:
        outputBuffer = ByteBuffer.allocate(BlobPropertiesSerDe.getBlobPropertiesSerDeSize(blobProperties));
        BlobPropertiesSerDe.serializeBlobProperties(outputBuffer, blobProperties);
        outputBuffer.flip();
        break;
    }
    return outputBuffer;
  }

  /**
   * Verify {@link BlobProperties} for its constituent values
   * @param blobProperties the {@link BlobProperties} that needs to be compared against
   * @param blobSize the size of the blob
   * @param serviceId the serviceId associated with the {@link BlobProperties}
   * @param ownerId the ownerId associated with the {@link BlobProperties}
   * @param contentType the contentType associated with the {@link BlobProperties}
   * @param isPrivate refers to whether the blob is private or not
   * @param ttlInSecs the time to live associated with the {@link BlobProperties} in secs
   * @param accountId accountId of the user who uploaded the blob
   * @param containerId containerId of the blob
   * @param isEncrypted whether the blob is encrypted
   * @param externalAssetTag the externalAssetTag of the blob.
   * @param contentEncoding the field to identify if the blob is compressed.
   * @param filename the name of the file.
   * @param reservedMetadataBlobId the reserved metadata blob id for chunked uploads or stitched blobs. Can be {@code null}.
   */
  private void verifyBlobProperties(BlobProperties blobProperties, long blobSize, String serviceId, String ownerId,
      String contentType, boolean isPrivate, long ttlInSecs, short accountId, short containerId, boolean isEncrypted,
      String externalAssetTag, String contentEncoding, String filename, String reservedMetadataBlobId) {
    assertEquals(blobProperties.getBlobSize(), blobSize);
    assertEquals(blobProperties.getServiceId(), serviceId);
    assertEquals(blobProperties.getOwnerId(), ownerId);
    assertEquals(blobProperties.getContentType(), contentType);
    assertEquals(blobProperties.isPrivate(), isPrivate);
    assertEquals(blobProperties.getTimeToLiveInSeconds(), ttlInSecs);
    assertEquals("AccountId mismatch ", accountId, blobProperties.getAccountId());
    assertEquals("ContainerId mismatch ", containerId, blobProperties.getContainerId());
    assertEquals(isEncrypted, blobProperties.isEncrypted());
    assertEquals("externalAssetTag mismatch", externalAssetTag, blobProperties.getExternalAssetTag());
    assertEquals("contentEncoding mismatch", contentEncoding, blobProperties.getContentEncoding());
    assertEquals("filename mismatch", filename, blobProperties.getFilename());
    assertEquals("reserved metadata id mismatch", reservedMetadataBlobId, blobProperties.getReservedMetadataBlobId());
  }
}
