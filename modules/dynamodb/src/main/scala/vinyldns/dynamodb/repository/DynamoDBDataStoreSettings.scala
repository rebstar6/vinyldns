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

package vinyldns.dynamodb.repository

import vinyldns.core.repository.{Repository, RepositoryName}
import vinyldns.core.repository.RepositoryName.RepositoryName

import scala.reflect.ClassTag

final case class DynamoDBDataStoreSettings(
    key: String,
    secret: String,
    endpoint: String,
    region: String)

final case class DynamoDBRepositorySettings(
    tableName: String,
    provisionedReads: Long,
    provisionedWrites: Long)

final case class RepositoriesGeneric[A](
    user: Option[A],
    group: Option[A],
    membership: Option[A],
    groupChange: Option[A],
    recordSet: Option[A],
    recordChange: Option[A],
    zoneChange: Option[A],
    zone: Option[A],
    batchChange: Option[A]) {

  private lazy val asMap: Map[RepositoryName, A] =
    List(
      user.map(RepositoryName.user -> _),
      group.map(RepositoryName.group -> _),
      membership.map(RepositoryName.membership -> _),
      groupChange.map(RepositoryName.groupChange -> _),
      recordSet.map(RepositoryName.recordSet -> _),
      recordChange.map(RepositoryName.recordChange -> _),
      zoneChange.map(RepositoryName.zoneChange -> _),
      zone.map(RepositoryName.zone -> _),
      batchChange.map(RepositoryName.batchChange -> _)
    ).flatten.toMap

  def keys: Set[RepositoryName] = asMap.keySet

  def get[A <: Repository: ClassTag](name: RepositoryName): Option[A] =
    asMap.get(name).flatMap {
      case a: A => Some(a)
      case _ => None
    }

  def mapWith[T](fn: A => T): RepositoriesGeneric[T] =
    RepositoriesGeneric[T](
      user.map(fn),
      group.map(fn),
      membership.map(fn),
      groupChange.map(fn),
      recordSet.map(fn),
      recordChange.map(fn),
      zoneChange.map(fn),
      zone.map(fn),
      batchChange.map(fn)
    )
}
