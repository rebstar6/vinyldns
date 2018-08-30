package vinyldns.api.domain.batch

import vinyldns.api.domain.batch.BatchChangeInterfaces.ValidatedBatch
import vinyldns.api.domain.batch.BatchTransformations.ChangeForValidation


/* Error response options */
sealed trait BatchChangeErrorResponse
// This separates error by change requested
final case class InvalidBatchChangeResponses(
                                              changeRequests: List[ChangeInput],
                                              changeRequestResponses: ValidatedBatch[ChangeForValidation])
  extends BatchChangeErrorResponse
// The request itself is invalid in this case, so we fail fast
final case class ChangeLimitExceeded(limit: Int) extends BatchChangeErrorResponse {
  def message: String = s"Cannot request more than $limit changes in a single batch change request"
}

final case class BatchChangeIsEmpty(limit: Int) extends BatchChangeErrorResponse {
  def message: String =
    s"Batch change contained no changes. Batch change must have at least one change, up to a maximum of $limit changes."
}

final case class BatchChangeNotFound(id: String) extends BatchChangeErrorResponse {
  def message: String = s"Batch change with id $id cannot be found"
}
final case class UserNotAuthorizedError(itemId: String) extends BatchChangeErrorResponse {
  def message: String = s"User does not have access to item $itemId"
}
final case class BatchConversionError(change: SingleChange) extends BatchChangeErrorResponse {
  def message: String =
    s"""Batch conversion for processing failed to convert change with name "${change.inputName}"
       |and type "${change.typ}"""".stripMargin
}
final case class UnknownConversionError(message: String) extends BatchChangeErrorResponse
