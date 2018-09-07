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

import cats.implicits._
import cats.effect.IO
import org.slf4j.LoggerFactory
import vinyldns.core.repository.{Repository, _}
import pureconfig.module.catseffect.loadConfigF
import vinyldns.core.repository.RepositoryName._

//import scala.collection.JavaConverters._

class DynamoDBDataStoreProvider extends DataStoreProvider {

  type DynamoDBRepositoriesSettings = RepositoriesGeneric[DynamoDBRepositorySettings]

  private val logger = LoggerFactory.getLogger("DynamoDBDataStoreProvider")
  private val implementedRepositories =
    Set(user, group, membership, groupChange, recordSet, recordChange, zoneChange)

  def load(config: DataStoreConfig): IO[DataStore] = {
    for {
      settingsConfig <- loadConfigF[IO, DynamoDBDataStoreSettings](config.settings)
      repoConfigs <- loadRepoConfigs(config.repositories)
      _ <- validateRepos(config.repositories)

      //      _ <- runDBMigrations(settingsConfig)
      //      _ <- setupDBConnection(settingsConfig)
      //      store <- initializeRepos()
    } yield settingsConfig

    IO.raiseError(new RuntimeException("sdf"))
  }

  def validateRepos(reposConfig: RepositoriesConfig): IO[Unit] = {
    val invalid = reposConfig.keys.diff(implementedRepositories)

    if (invalid.isEmpty) {
      IO.unit
    } else {
      val error = s"Invalid config provided to dynamodb; unimplemented repos included: $invalid"
      IO.raiseError(DataStoreStartupError(error))
    }
  }

  def loadRepoConfigs(config: RepositoriesConfig): IO[DynamoDBRepositoriesSettings] = {
    logger.info("Running migrations to ready the databases")

    def loadConfigIfDefined(
        repositoryName: RepositoryName): IO[Option[DynamoDBRepositorySettings]] =
      config.get(repositoryName) match {
        case Some(c) => loadConfigF[IO, DynamoDBRepositorySettings](c).map(Some(_))
        case None => IO.pure(None)
      }

    (
      loadConfigIfDefined(user),
      loadConfigIfDefined(group),
      loadConfigIfDefined(membership),
      loadConfigIfDefined(groupChange),
      loadConfigIfDefined(recordSet),
      loadConfigIfDefined(recordChange),
      loadConfigIfDefined(zoneChange),
      loadConfigIfDefined(zone),
      loadConfigIfDefined(batchChange)
    ).parMapN { RepositoriesGeneric[DynamoDBRepositorySettings] }
  }

  def initializeSingleRepos(dataStoreSettings: DynamoDBDataStoreSettings,
                            repoSettings: DynamoDBRepositoriesSettings): IO[DataStore] = {



    repoSettings.user.map(new DynamoDBUserRepository(dataStoreSettings, _))

    def initializeRepoIfOn[T <: Repository](repositoryName: RepositoryName): IO[Option[T]] =
      repoSettings.get[DynamoDBRepositorySettings](repositoryName) match {
        case Some(c) => new T(dataStoreSettings, c)
        case None => IO.pure(None)
      }



  }

//
//  def initializeRepos(): IO[DataStore] = IO {
//    val zones = Some(new JdbcZoneRepository())
//    val batchChanges = Some(new JdbcBatchChangeRepository())
//    new DataStore(zoneRepository = zones, batchChangeRepository = batchChanges)
//  }
//
//  def runDBMigrations(settings: MySqlDataStoreSettings): IO[Unit] = IO {
//    // Migration needs to happen on the base URL, not the table URL, thus the separate source
//    lazy val migrationDataSource: DataSource = {
//      val ds = new HikariDataSource()
//      ds.setDriverClassName(settings.driver)
//      ds.setJdbcUrl(settings.migrationUrl)
//      ds.setUsername(settings.user)
//      ds.setPassword(settings.password)
//      // migrations happen once on startup; without these settings the default number of connections
//      // will be created and maintained even though this datasource is no longer needed post-migration
//      ds.setMaximumPoolSize(3)
//      ds.setMinimumIdle(0)
//      ds
//    }
//
//    logger.info("Running migrations to ready the databases")
//
//    val migration = new Flyway()
//    migration.setDataSource(migrationDataSource)
//    // flyway changed the default schema table name in v5.0.0
//    // this allows to revert to an old naming convention if needed
//    settings.migrationSchemaTable.foreach { tableName =>
//      migration.setTable(tableName)
//    }
//
//    val placeholders = Map("dbName" -> settings.name)
//    migration.setPlaceholders(placeholders.asJava)
//    migration.setSchemas(settings.name)
//
//    // Runs flyway migrations
//    migration.migrate()
//    logger.info("migrations complete")
//  }
//
//  def setupDBConnection(settings: MySqlDataStoreSettings): IO[Unit] = IO {
//    val dataSource: DataSource = {
//      val ds = new HikariDataSource()
//      ds.setDriverClassName(settings.driver)
//      ds.setJdbcUrl(settings.url)
//      ds.setUsername(settings.user)
//      ds.setPassword(settings.password)
//      ds.setConnectionTimeout(settings.connectionTimeoutMillis)
//      ds.setMaximumPoolSize(settings.poolMaxSize)
//      ds.setMaxLifetime(settings.maxLifeTime)
//      ds.setRegisterMbeans(true)
//      ds
//    }
//
//    logger.info("configuring connection pool")
//
//    // Configure the connection pool
//    ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))
//
//    logger.info("setting up databases")
//
//    // Sets up all databases with scalikejdbc
//    DBs.setupAll()
//
//    logger.info("database init complete")
//  }

}
