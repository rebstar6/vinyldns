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

package modules

import com.google.inject.{AbstractModule, Provides}
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer
import org.pac4j.core.authorization.generator.AuthorizationGenerator
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.context.WebContext
import org.pac4j.core.profile.CommonProfile
import org.pac4j.oidc.client.AzureAdClient
import org.pac4j.oidc.config.AzureAdOidcConfiguration
import org.pac4j.oidc.profile.azuread.AzureAdProfile
import org.pac4j.play.http.PlayHttpActionAdapter
import org.pac4j.play.scala.{
  DefaultSecurityComponents,
  Pac4jScalaTemplateHelper,
  SecurityComponents
}
import org.pac4j.play.store.{PlayCookieSessionStore, PlaySessionStore, ShiroAesDataEncrypter}
import org.pac4j.play.{CallbackController, LogoutController}
import play.api.{Configuration, Environment}

/**
  * Guice DI module to be included in application.conf
  */
// $COVERAGE-OFF$
class SecurityModule(environment: Environment, configuration: Configuration)
    extends AbstractModule {

  lazy val discoveryUrl: String = configuration.get[String]("oidc.metadata-url")
  lazy val clientId: String = configuration.get[String]("oidc.client-id")
  lazy val baseUrl: String = configuration.get[String]("oidc.redirect-uri")
  lazy val secret: String = configuration.get[String]("oidc.secret")
  lazy val scope: String = configuration.get[String]("oidc.scope")
  lazy val logoutUrl: String = configuration.get[String]("oidc.logout-url")
  lazy val tenantId: String = configuration.get[String]("oidc.tenant-id")
  lazy val oidcUsernameField: String =
    configuration.getOptional[String]("oidc.jwt-username-field").getOrElse("username")

  val oidcEnabled: Boolean = configuration.getOptional[Boolean]("oidc.enabled").getOrElse(false)

  override def configure(): Unit = {
    val sKey = configuration.get[String]("play.http.secret.key").substring(0, 16)
    val dataEncrypter = new ShiroAesDataEncrypter(sKey)
    val playSessionStore = new PlayCookieSessionStore(dataEncrypter)
    bind(classOf[PlaySessionStore]).toInstance(playSessionStore)

    bind(classOf[Pac4jScalaTemplateHelper[CommonProfile]])

    // security components used in controllers
    bind(classOf[SecurityComponents]).to(classOf[DefaultSecurityComponents])

    // callback
    val callbackController = new CallbackController()
    callbackController.setDefaultUrl("/")
    callbackController.setMultiProfile(false)
    callbackController.setDefaultClient("AzureAdClient")
    bind(classOf[CallbackController]).toInstance(callbackController)

    // logout
    val logoutController = new LogoutController()
    logoutController.setDefaultUrl("/")
    logoutController.setLogoutUrlPattern("http://localhost:9001/.*")
    bind(classOf[LogoutController]).toInstance(logoutController)

  }

  class RoleAdminAuthGenerator extends AuthorizationGenerator[AzureAdProfile] {
    def generate(context: WebContext, profile: AzureAdProfile): AzureAdProfile = {
      profile.addRole("ROLE_ADMIN")
      profile
    }
  }

  @Provides
  def provideAzureAdClient: AzureAdClient = {
    val azureAdConfiguration = new AzureAdOidcConfiguration()
    azureAdConfiguration.setClientId(clientId)
    azureAdConfiguration.setSecret(secret)
    azureAdConfiguration.setDiscoveryURI(discoveryUrl)
    azureAdConfiguration.setLogoutUrl(logoutUrl)
    azureAdConfiguration.setTenant(tenantId)
    val azureAdClient = new AzureAdClient(azureAdConfiguration)
    azureAdClient.addAuthorizationGenerator(new RoleAdminAuthGenerator)
    azureAdClient
  }

  @Provides
  def provideConfig(azureAdClient: AzureAdClient): Config = {
    val clients = new Clients(baseUrl + "/callback", azureAdClient)

    val config = new Config(clients)
    config.addAuthorizer("admin", new RequireAnyRoleAuthorizer[Nothing]("ROLE_ADMIN"))
    config.setHttpActionAdapter(new PlayHttpActionAdapter())
    config
  }

}
// $COVERAGE-ON$
