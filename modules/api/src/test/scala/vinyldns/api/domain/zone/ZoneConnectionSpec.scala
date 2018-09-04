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

package vinyldns.api.domain.zone

import cats.scalatest.EitherMatchers
import org.scalacheck.Gen._
import org.scalacheck._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{Matchers, PropSpec}
import vinyldns.api.VinylDNSTestData
import vinyldns.api.domain.DomainValidations._
import vinyldns.core.crypto.CryptoAlgebra
import vinyldns.core.domain.zone.ZoneConnection

class ZoneConnectionSpec
    extends PropSpec
    with Matchers
    with GeneratorDrivenPropertyChecks
    with EitherMatchers
    with VinylDNSTestData {
  val validName: String = "zoneConnectionName"
  val keyName: String = "zoneConnectionKeyName"
  val keyValue: String = "zoneConnectionKey"

  val validAddress1 = "0.0.0.0"
  val validAddress2 = "255.255.255.255"
  val validDomain1: String = "www.test.domain.name.com."
  val validDomain2: String = "A."
  val validAddrs = List(validAddress1, validAddress2, validDomain1, validDomain2)

  val validPort1: Int = 0
  val validPort2: Int = 65535
  val validPorts: List[String] = List(validPort1.toString, validPort2.toString, "")

  val invalidName: String = "a" * 256

  val invalidDomain = "-hello."
  val invalidAddress = "256.256.256.256"
  val invalidAddrs = List(invalidDomain, invalidAddress)

  val invalidPort1 = "-1"
  val invalidPort2 = "65536"
  val invalidPortNums = List(invalidPort1, invalidPort2)

  val invalidPortParams = List("a", "invalidport")

  val testCrypto = new CryptoAlgebra {
    def encrypt(value: String): String = "encrypted!"
    def decrypt(value: String): String = "decrypted!"
  }

  object ZoneConnectionGenerator {
    val connectionNameGen: Gen[String] = for {
      numberWithinRange <- choose(1, HOST_MAX_LENGTH)
      variableLengthString <- listOfN(numberWithinRange, alphaNumChar).map(_.mkString)
    } yield variableLengthString
  }

  def buildConnectionCombos(addrs: List[String], ports: List[String]): List[String] =
    for {
      a <- addrs
      p <- ports
    } yield s"$a:$p"

  property("ZoneConnection should encrypt clear connections") {
    val test = ZoneConnection("vinyldns.", "vinyldns.", "nzisn+4G2ldMn0q1CV3vsg==", "10.1.1.1")

    test.encrypted(testCrypto).key shouldBe "encrypted!"
  }

  property("ZoneConnection should decrypt connections") {
    val test = ZoneConnection("vinyldns.", "vinyldns.", "nzisn+4G2ldMn0q1CV3vsg==", "10.1.1.1")
    val decrypted = test.decrypted(testCrypto)

    decrypted.key shouldBe "decrypted!"
  }
}
