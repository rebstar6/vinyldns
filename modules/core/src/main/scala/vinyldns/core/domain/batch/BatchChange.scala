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

package vinyldns.core.domain.batch

import java.util.UUID

import org.joda.time.DateTime
import vinyldns.core.domain.batch.BatchChangeStatus.BatchChangeStatus
import vinyldns.core.domain.batch.BatchChangeApprovalStatus.BatchChangeApprovalStatus

trait BatchChange {
  val userId: String
  val userName: String
  val comments: Option[String]
  val createdTimestamp: DateTime
  val changes: List[SingleChange]
  val approvalStatus: BatchChangeApprovalStatus
  val ownerGroupId: Option[String]
  val reviewerId: Option[String]
  val reviewComment: Option[String]
  val reviewTimestamp: Option[DateTime]
  val id: String
  val status: BatchChangeStatus

  def statusFromChanges: BatchChangeStatus = {
    val singleStatuses = changes.map(_.status)
    val hasPending = singleStatuses.contains(SingleChangeStatus.Pending)
    val hasFailed = singleStatuses.contains(SingleChangeStatus.Failed)
    val hasComplete = singleStatuses.contains(SingleChangeStatus.Complete)

    BatchChangeStatus.fromSingleStatuses(hasPending, hasFailed, hasComplete)
  }
}

object BatchChange {

  def apply(userId: String,
            userName: String,
            comments: Option[String],
            createdTimestamp: DateTime,
            changes: List[SingleChange],
            approvalStatus: BatchChangeApprovalStatus,
            ownerGroupId: Option[String] = None,
            reviewerId: Option[String] = None,
            reviewComment: Option[String] = None,
            reviewTimestamp: Option[DateTime] = None,
            id: String = UUID.randomUUID().toString): BatchChange =
    approvalStatus match {
      case BatchChangeApprovalStatus.PendingApproval || BatchChangeApprovalStatus.ManuallyRejected =>
        UnapprovedBatchChange(userId, userName, comments, createdTimestamp,
          changes, approvalStatus, ownerGroupId, reviewerId, reviewComment, reviewTimestamp, id)
      case BatchChangeApprovalStatus.AutoApproved || BatchChangeApprovalStatus.ManuallyApproved =>
        // Note - this assumes that all single changes in a batch can be converted to approved if the status is
        // an approved status. This is the case, though its not the best doing this here
        val changesAsApproved = changes.collect {
          case approved: ApprovedSingleChange => approved
        }
        ApprovedBatchChange(userId, userName, comments, createdTimestamp,
          changesAsApproved, approvalStatus, ownerGroupId, reviewerId, reviewComment, reviewTimestamp, id)
    }
}

case class  ApprovedBatchChange(
    userId: String,
    userName: String,
    comments: Option[String],
    createdTimestamp: DateTime,
    changes: List[ApprovedSingleChange],
    approvalStatus: BatchChangeApprovalStatus,
    ownerGroupId: Option[String] = None,
    reviewerId: Option[String] = None,
    reviewComment: Option[String] = None,
    reviewTimestamp: Option[DateTime] = None,
    id: String = UUID.randomUUID().toString) extends BatchChange {

  val status: BatchChangeStatus = statusFromChanges
}

case class UnapprovedBatchChange(
                                  userId: String,
                                  userName: String,
                                  comments: Option[String],
                                  createdTimestamp: DateTime,
                                  changes: List[SingleChange],
                                  approvalStatus: BatchChangeApprovalStatus,
                                  ownerGroupId: Option[String] = None,
                                  reviewerId: Option[String] = None,
                                  reviewComment: Option[String] = None,
                                  reviewTimestamp: Option[DateTime] = None,
                                  id: String = UUID.randomUUID().toString) extends BatchChange {

  val status: BatchChangeStatus = approvalStatus match {
    case BatchChangeApprovalStatus.PendingApproval => BatchChangeStatus.Pending
    case BatchChangeApprovalStatus.ManuallyRejected => BatchChangeStatus.Failed
      // should not hit this case - if were here, the batch should instead be an ApprovedBatchChange
    case _ => statusFromChanges
  }
}

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

object BatchChangeApprovalStatus extends Enumeration {
  type BatchChangeApprovalStatus = Value
  val AutoApproved, PendingApproval, ManuallyApproved, ManuallyRejected = Value
}

case class BatchChangeInfo(
                            userId: String,
                            userName: String,
                            comments: Option[String],
                            createdTimestamp: DateTime,
                            changes: List[SingleChange],
                            ownerGroupId: Option[String],
                            id: String,
                            status: BatchChangeStatus,
                            ownerGroupName: Option[String]
)

object BatchChangeInfo {
  def apply(batchChange: BatchChange, ownerGroupName: Option[String] = None): BatchChangeInfo = {
    import batchChange._
    BatchChangeInfo(
      userId,
      userName,
      comments,
      createdTimestamp,
      changes,
      ownerGroupId,
      id,
      status,
      ownerGroupName
    )
  }
}
