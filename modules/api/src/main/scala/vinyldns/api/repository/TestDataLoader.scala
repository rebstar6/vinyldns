package vinyldns.api.repository

import cats.effect.IO
import cats.implicits._
import org.joda.time.DateTime
import vinyldns.api.VinylDNSConfig
import vinyldns.api.domain.membership._
import vinyldns.core.crypto.Crypto

object TestDataLoader {

  def loadTestData(userRepo: UserRepository, groupRepo: GroupRepository,
                   membershipRepo: MembershipRepository): IO[Unit] =
    for {
      _ <- loadUserTestData(userRepo)
      _ <- loadGroupTestData(groupRepo)
      _ <- loadMembershipTestData(membershipRepo)
    } yield ()


  final val testUser = User(
    userName = "testuser",
    id = "testuser",
    created = DateTime.now.secondOfDay().roundFloorCopy(),
    accessKey = "testUserAccessKey",
    secretKey = "testUserSecretKey",
    firstName = Some("Test"),
    lastName = Some("User"),
    email = Some("test@test.com")
  )
  final val okUser = User(
    userName = "ok",
    id = "ok",
    created = DateTime.now.secondOfDay().roundFloorCopy(),
    accessKey = "okAccessKey",
    secretKey = "okSecretKey",
    firstName = Some("ok"),
    lastName = Some("ok"),
    email = Some("test@test.com")
  )
  final val dummyUser = User(
    userName = "dummy",
    id = "dummy",
    created = DateTime.now.secondOfDay().roundFloorCopy(),
    accessKey = "dummyAccessKey",
    secretKey = "dummySecretKey")
  final val listOfDummyUsers: List[User] = List.range(0, 200).map { runner =>
    User(
      userName = "name-dummy%03d".format(runner),
      id = "dummy%03d".format(runner),
      created = DateTime.now.secondOfDay().roundFloorCopy(),
      accessKey = "dummy",
      secretKey = "dummy"
    )
  }
  final val listGroupUser = User(
    userName = "list-group-user",
    id = "list-group-user",
    created = DateTime.now.secondOfDay().roundFloorCopy(),
    accessKey = "listGroupAccessKey",
    secretKey = "listGroupSecretKey",
    firstName = Some("list-group"),
    lastName = Some("list-group"),
    email = Some("test@test.com")
  )

  final val listZonesUser = User(
    userName = "list-zones-user",
    id = "list-zones-user",
    created = DateTime.now.secondOfDay().roundFloorCopy(),
    accessKey = "listZonesAccessKey",
    secretKey = "listZonesSecretKey",
    firstName = Some("list-zones"),
    lastName = Some("list-zones"),
    email = Some("test@test.com")
  )

  final val zoneHistoryUser = User(
    userName = "history-user",
    id = "history-id",
    created = DateTime.now.secondOfDay().roundFloorCopy(),
    accessKey = "history-key",
    secretKey = "history-secret",
    firstName = Some("history-first"),
    lastName = Some("history-last"),
    email = Some("history@history.com")
  )

  final val listBatchChangeSummariesUser = User(
    userName = "list-batch-summaries-user",
    id = "list-batch-summaries-id",
    created = DateTime.now.secondOfDay().roundFloorCopy(),
    accessKey = "listBatchSummariesAccessKey",
    secretKey = "listBatchSummariesSecretKey",
    firstName = Some("list-batch-summaries"),
    lastName = Some("list-batch-summaries"),
    email = Some("test@test.com")
  )

  final val listZeroBatchChangeSummariesUser = User(
    userName = "list-zero-summaries-user",
    id = "list-zero-summaries-id",
    created = DateTime.now.secondOfDay().roundFloorCopy(),
    accessKey = "listZeroSummariesAccessKey",
    secretKey = "listZeroSummariesSecretKey",
    firstName = Some("list-zero-summaries"),
    lastName = Some("list-zero-summaries"),
    email = Some("test@test.com")
  )

  final val okGroup1 = Group(
    "ok-group",
    "test@test.com",
    memberIds = Set("ok"),
    adminUserIds = Set("ok"),
    id = "ok-group")
  final val okGroup2 =
    Group("ok", "test@test.com", memberIds = Set("ok"), adminUserIds = Set("ok"), id = "ok")

  def loadGroupTestData(repository: GroupRepository): IO[List[Group]] =
    List(okGroup1, okGroup2).map(repository.save(_)).parSequence

  def loadUserTestData(repository: UserRepository): IO[List[User]] =
    (testUser :: okUser :: dummyUser :: listGroupUser :: listZonesUser :: listBatchChangeSummariesUser ::
      listZeroBatchChangeSummariesUser :: zoneHistoryUser :: listOfDummyUsers).map { user =>
      val encrypted =
        if (VinylDNSConfig.encryptUserSecrets)
          user.copy(secretKey = Crypto.encrypt(user.secretKey))
        else user
      repository.save(encrypted)
    }.parSequence


  def loadMembershipTestData(repository: MembershipRepository): IO[Set[Set[String]]] =
    List("ok-group", "ok").map(repository.addMembers(_, Set("ok"))).parSequence.map(_.toSet)
}
