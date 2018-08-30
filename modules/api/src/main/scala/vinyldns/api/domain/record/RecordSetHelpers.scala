package vinyldns.api.domain.record

import vinyldns.api.domain.dns.DnsConversions

object RecordSetHelpers {

  def matches(left: RecordSet, right: RecordSet, zoneName: String): Boolean = {

    val isSame = for {
      thisDnsRec <- DnsConversions.toDnsRecords(left, zoneName)
      otherDnsRec <- DnsConversions.toDnsRecords(right, zoneName)
    } yield {
      val rsMatch = otherDnsRec.toSet == thisDnsRec.toSet
      val namesMatch = matchesNameQualification(left, right)
      val headersMatch = left.ttl == right.ttl && left.typ == right.typ
      rsMatch && namesMatch && headersMatch
    }

    isSame.getOrElse(false)
  }

  def matchesNameQualification(left: RecordSet, right: RecordSet): Boolean =
    isFullyQualified(left.name) == isFullyQualified(right.name)

  def isFullyQualified(name: String): Boolean = name.endsWith(".")

}
