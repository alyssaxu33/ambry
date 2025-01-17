/**
 * Copyright 2017 LinkedIn Corp. All rights reserved.
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
package com.github.ambry.store;

import com.github.ambry.config.StoreConfig;
import com.github.ambry.utils.Pair;
import com.github.ambry.utils.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * StatsBasedCompactionPolicy calculates the cost benefit ratio for every potential candidate to compact and finds the
 * best one to be compacted to be returned for {@link CompactionPolicy#getCompactionDetails(long, long, long, long, List, BlobStoreStats, String)}
 */
class StatsBasedCompactionPolicy implements CompactionPolicy {

  final static long ERROR_MARGIN_MS = 1000 * 60 * 60;
  private final Time time;
  private final StoreConfig storeConfig;
  private final long messageRetentionTimeInMs;
  private static final Logger logger = LoggerFactory.getLogger(StatsBasedCompactionPolicy.class);

  StatsBasedCompactionPolicy(StoreConfig storeConfig, Time time) {
    this.storeConfig = storeConfig;
    this.time = time;
    this.messageRetentionTimeInMs = TimeUnit.MINUTES.toMillis(storeConfig.storeDeletedMessageRetentionMinutes);
  }

  @Override
  public CompactionDetails getCompactionDetails(long totalCapacity, long usedCapacity, long segmentCapacity,
      long segmentHeaderSize, List<LogSegmentName> logSegmentsNotInJournal, BlobStoreStats blobStoreStats,
      String dataDir) throws StoreException {
    CompactionDetails details = null;
    logger.debug("UsedCapacity {} vs TotalCapacity {}", usedCapacity, totalCapacity);
    if (usedCapacity >= (storeConfig.storeMinUsedCapacityToTriggerCompactionInPercentage / 100.0) * totalCapacity) {
      if (logSegmentsNotInJournal != null) {
        Pair<Long, NavigableMap<LogSegmentName, Long>> validDataSizeByLogSegment =
            blobStoreStats.getValidDataSizeByLogSegment(
                new TimeRange(time.milliseconds() - messageRetentionTimeInMs - ERROR_MARGIN_MS, ERROR_MARGIN_MS));
        logger.info("Valid data size for {} from BlobStoreStats {} ", dataDir, validDataSizeByLogSegment);
        NavigableMap<LogSegmentName, Long> potentialLogSegmentValidSizeMap = validDataSizeByLogSegment.getSecond()
            .subMap(logSegmentsNotInJournal.get(0), true,
                logSegmentsNotInJournal.get(logSegmentsNotInJournal.size() - 1), true);

        CostBenefitInfo bestCandidateToCompact =
            getBestCandidateToCompact(potentialLogSegmentValidSizeMap, segmentCapacity, segmentHeaderSize,
                blobStoreStats.getMaxBlobSize());
        if (bestCandidateToCompact != null) {
          details =
              new CompactionDetails(validDataSizeByLogSegment.getFirst(), bestCandidateToCompact.getSegmentsToCompact(),
                  bestCandidateToCompact);
          logger.info("Best candidate to compact {} on {}", bestCandidateToCompact, dataDir);
        } else {
          logger.trace("No best candidate found");
        }
      }
    }
    return details;
  }

  /**
   * Finds the best candidate to compact by finding the candidate with the best cost benefit ratio
   * @param validDataPerLogSegments the valid data size for each log segment in the form of a {@link NavigableMap} of segment names to
   * valid data sizes.
   * @param segmentCapacity Segment capacity of one {@link LogSegment}
   * @param segmentHeaderSize Segment header size of a {@link LogSegment}
   * @param maxBlobSize The max blob size
   * @return the {@link CostBenefitInfo} for the best candidate to compact. {@code null} if there isn't any.
   */
  private CostBenefitInfo getBestCandidateToCompact(NavigableMap<LogSegmentName, Long> validDataPerLogSegments,
      long segmentCapacity, long segmentHeaderSize, long maxBlobSize) {
    Map.Entry<LogSegmentName, Long> firstEntry = validDataPerLogSegments.firstEntry();
    Map.Entry<LogSegmentName, Long> lastEntry = validDataPerLogSegments.lastEntry();
    CostBenefitInfo bestCandidateToCompact = null;
    while (firstEntry != null) {
      Map.Entry<LogSegmentName, Long> endEntry = lastEntry;
      while (endEntry != null && firstEntry.getKey().compareTo(endEntry.getKey()) <= 0) {
        CostBenefitInfo costBenefitInfo =
            getCostBenefitInfo(firstEntry.getKey(), endEntry.getKey(), validDataPerLogSegments, segmentCapacity,
                segmentHeaderSize, maxBlobSize);
        if (costBenefitInfo.getBenefit() >= storeConfig.storeMinLogSegmentCountToReclaimToTriggerCompaction && (
            bestCandidateToCompact == null
                || costBenefitInfo.getCostBenefitRatio().compareTo(bestCandidateToCompact.getCostBenefitRatio()) < 0)) {
          bestCandidateToCompact = costBenefitInfo;
          logger.trace("Updating best candidate to compact to {} ", bestCandidateToCompact);
        }
        endEntry = validDataPerLogSegments.lowerEntry(endEntry.getKey());
      }
      firstEntry = validDataPerLogSegments.higherEntry(firstEntry.getKey());
    }
    return bestCandidateToCompact;
  }

  /**
   * Calculates the {@link CostBenefitInfo} for the candidate to compact
   * @param firstLogSegmentName first log segment name of the candidate to compact
   * @param lastLogSegmentName last log segment name of the candidate to compact
   * @param validDataSizePerLogSegment the valid data size for each log segment in the form of a {@link NavigableMap} of segment names to
   * valid data sizes.
   * @param segmentCapacity Segment capacity of one {@link LogSegment}
   * @param segmentHeaderSize Segment header size of a {@link LogSegment}
   * @param maxBlobSize The max blob size.
   * @return the {@link CostBenefitInfo} for the candidate to compact.
   */
  private CostBenefitInfo getCostBenefitInfo(LogSegmentName firstLogSegmentName, LogSegmentName lastLogSegmentName,
      NavigableMap<LogSegmentName, Long> validDataSizePerLogSegment, long segmentCapacity, long segmentHeaderSize,
      long maxBlobSize) {
    // cost = total valid data size (or data that needs to be copied over)
    // benefit = no of segments reclaimed = (total source segments - total target segments)
    // total target segments = Ceil (total cost / (segment capacity - segment header - max blob size))
    long totalCost = 0;
    int totalSegmentsToBeCompacted = 0;
    LogSegmentName curSegmentName = firstLogSegmentName;
    while (!curSegmentName.equals(lastLogSegmentName)) {
      totalCost += validDataSizePerLogSegment.get(curSegmentName);
      curSegmentName = validDataSizePerLogSegment.higherKey(curSegmentName);
      totalSegmentsToBeCompacted++;
    }
    // add the last log segment
    totalCost += validDataSizePerLogSegment.get(curSegmentName);
    totalSegmentsToBeCompacted++;
    long maxCapacityPerSegment = segmentCapacity - segmentHeaderSize - maxBlobSize;
    int benefit = totalSegmentsToBeCompacted - (int) Math.ceil(totalCost / (maxCapacityPerSegment * 1.0));
    return new CostBenefitInfo(new ArrayList<>(
        validDataSizePerLogSegment.subMap(firstLogSegmentName, true, lastLogSegmentName, true).keySet()), totalCost,
        benefit);
  }
}
