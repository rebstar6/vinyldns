/*
 * Copyright 2018 Comcast Cable Communications Management, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vinyldns.api.domain.batch

import java.util.UUID

import org.joda.time.DateTime
import vinyldns.api.domain.batch.BatchChangeStatus.BatchChangeStatus
import vinyldns.api.domain.batch.SingleChangeStatus.SingleChangeStatus
import vinyldns.api.domain.record.RecordType._
import vinyldns.api.domain.record.RecordData

case class BatchChangeSummary(
    userId: String,
    userName: String,
    comments: Option[String],
    createdTimestamp: DateTime,
    totalChanges: Int,
    status: BatchChangeStatus,
    id: String = UUID.randomUUID().toString) {}

object BatchChangeSummary {
  def apply(batchchange: BatchChange): BatchChangeSummary =
    BatchChangeSummary(
      batchchange.userId,
      batchchange.userName,
      batchchange.comments,
      batchchange.createdTimestamp,
      batchchange.changes.length,
      batchchange.status,
      batchchange.id
    )
}

case class BatchChangeSummaryList(
    batchChanges: List[BatchChangeSummary],
    startFrom: Option[Int] = None,
    nextId: Option[Int] = None,
    maxItems: Int = 100)

case class BatchChange(
    userId: String,
    userName: String,
    comments: Option[String],
    createdTimestamp: DateTime,
    changes: List[SingleChange],
    id: String = UUID.randomUUID().toString) {
  val status: BatchChangeStatus = {
    val singleStatuses = changes.map(_.status)
    val hasPending = singleStatuses.contains(SingleChangeStatus.Pending)
    val hasFailed = singleStatuses.contains(SingleChangeStatus.Failed)
    val hasComplete = singleStatuses.contains(SingleChangeStatus.Complete)

    BatchChangeStatus.fromSingleStatuses(hasPending, hasFailed, hasComplete)
  }
}

object SupportedBatchChangeRecordTypes {
  val supportedTypes = Set(A, AAAA, CNAME, PTR, TXT, MX)
  def get: Set[RecordType] = supportedTypes
}

sealed trait SingleChange {
  val id: String
  val status: SingleChangeStatus
  val systemMessage: Option[String]
  val recordChangeId: Option[String]
  val recordSetId: Option[String]
  val zoneId: String
  val recordName: String
  val typ: RecordType
  val inputName: String
  val zoneName: String
  val recordKey = RecordKey(zoneId, recordName, typ)

  def withFailureMessage(error: String): SingleChange = this match {
    case add: SingleAddChange =>
      add.copy(status = SingleChangeStatus.Failed, systemMessage = Some(error))
    case delete: SingleDeleteChange =>
      delete.copy(status = SingleChangeStatus.Failed, systemMessage = Some(error))
  }

  def withProcessingError(message: Option[String], failedRecordChangeId: String): SingleChange =
    this match {
      case add: SingleAddChange =>
        add.copy(
          status = SingleChangeStatus.Failed,
          systemMessage = message,
          recordChangeId = Some(failedRecordChangeId))
      case delete: SingleDeleteChange =>
        delete.copy(
          status = SingleChangeStatus.Failed,
          systemMessage = message,
          recordChangeId = Some(failedRecordChangeId))
    }

  def complete(completeRecordChangeId: String, recordSetId: String): SingleChange = this match {
    case add: SingleAddChange =>
      add.copy(
        status = SingleChangeStatus.Complete,
        recordChangeId = Some(completeRecordChangeId),
        recordSetId = Some(recordSetId))
    case delete: SingleDeleteChange =>
      delete.copy(
        status = SingleChangeStatus.Complete,
        recordChangeId = Some(completeRecordChangeId),
        recordSetId = Some(recordSetId))
  }
}

final case class SingleAddChange(
    zoneId: String,
    zoneName: String,
    recordName: String,
    inputName: String,
    typ: RecordType,
    ttl: Long,
    recordData: RecordData,
    status: SingleChangeStatus,
    systemMessage: Option[String],
    recordChangeId: Option[String],
    recordSetId: Option[String],
    id: String = UUID.randomUUID().toString)
    extends SingleChange

final case class SingleDeleteChange(
    zoneId: String,
    zoneName: String,
    recordName: String,
    inputName: String,
    typ: RecordType,
    status: SingleChangeStatus,
    systemMessage: Option[String],
    recordChangeId: Option[String],
    recordSetId: Option[String],
    id: String = UUID.randomUUID().toString)
    extends SingleChange

/*
 - Pending if at least one change is still in pending state
 - Complete means all changes are in complete state
 - Failed means all changes are in failure state
 - PartialFailure means some have failed, the rest are complete
 */
object BatchChangeStatus extends Enumeration {
  type BatchChangeStatus = Value
  val Pending, Complete, Failed, PartialFailure = Value

  def fromSingleStatuses(
      hasPending: Boolean,
      hasFailed: Boolean,
      hasComplete: Boolean): BatchChangeStatus =
    (hasPending, hasFailed, hasComplete) match {
      case (true, _, _) => BatchChangeStatus.Pending
      case (_, true, true) => BatchChangeStatus.PartialFailure
      case (_, true, false) => BatchChangeStatus.Failed
      case _ => BatchChangeStatus.Complete
    }
}

/*
 - Pending has not yet been processed
 - Complete has been processed - must have recordChangeId
 - Failed had some error (see systemMessage) - may have recordChangeId, not required
 */
object SingleChangeStatus extends Enumeration {
  type SingleChangeStatus = Value
  val Pending, Complete, Failed = Value
}

case class RecordKey(zoneId: String, recordName: String, recordType: RecordType)

object RecordKey {
  def apply(zoneId: String, recordName: String, recordType: RecordType): RecordKey =
    new RecordKey(zoneId, recordName.toLowerCase, recordType)
}
