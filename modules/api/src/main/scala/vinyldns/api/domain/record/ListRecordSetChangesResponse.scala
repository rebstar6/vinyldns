package vinyldns.api.domain.record

import vinyldns.api.domain.zone.RecordSetChangeInfo

case class ListRecordSetChangesResponse(
                                         zoneId: String,
                                         recordSetChanges: List[RecordSetChangeInfo] = Nil,
                                         nextId: Option[String],
                                         startFrom: Option[String],
                                         maxItems: Int)

object ListRecordSetChangesResponse {
  def apply(
             zoneId: String,
             listResults: ListRecordSetChangesResults,
             info: List[RecordSetChangeInfo]): ListRecordSetChangesResponse =
    ListRecordSetChangesResponse(
      zoneId,
      info,
      listResults.nextId,
      listResults.startFrom,
      listResults.maxItems)
}
