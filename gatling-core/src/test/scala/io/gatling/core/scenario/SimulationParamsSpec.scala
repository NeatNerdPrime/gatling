/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.core.scenario

import java.util.concurrent.atomic.AtomicLong

import io.gatling.BaseSpec
import io.gatling.commons.util.Clock
import io.gatling.core.config.FrontLineProbeConfiguration
import io.gatling.core.controller.inject.{ InjectionProfile, Workload }
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine

import io.netty.channel.EventLoopGroup

class SimulationParamsSpec extends BaseSpec {

  private def newInjectionProfile(empty: Boolean): InjectionProfile =
    new InjectionProfile {
      override def totalUserCount: Option[Long] = None

      override def isEmpty: Boolean = empty

      override def workload(
          scenario: Scenario,
          userIdGen: AtomicLong,
          startTime: Long,
          eventLoopGroup: EventLoopGroup,
          statsEngine: StatsEngine,
          clock: Clock
      ): Workload = null

      //[fl]
      //
      //[fl]
    }

  private def newScenario(name: String, empty: Boolean): Scenario =
    new Scenario(
      name = name,
      entry = null,
      onStart = Session.Identity,
      onExit = Session.NothingOnExit,
      injectionProfile = newInjectionProfile(empty),
      ctx = null,
      children = Nil
    )

  "removeScenariosWithEmptyInjectionProfiles" should "remove empty root scenarios" in {
    val nonEmptyRootScenario = newScenario("scenario1", empty = false)
    val emptyRootScenario = newScenario("scenario2", empty = true)

    val (rootScenarios, childrenScenarios) = SimulationParams.removeScenariosWithEmptyInjectionProfiles(
      List(nonEmptyRootScenario, emptyRootScenario),
      Map(nonEmptyRootScenario.name -> Nil, emptyRootScenario.name -> Nil)
    )

    rootScenarios shouldBe List(nonEmptyRootScenario)
    childrenScenarios shouldBe Map(nonEmptyRootScenario.name -> Nil, emptyRootScenario.name -> Nil)
  }

  it should "move up empty scenarios' children" in {
    val nonEmptyRootScenario = newScenario("scenario1", empty = false)
    val emptyRootScenario = newScenario("scenario2", empty = true)

    val child1 = newScenario("child1", empty = false)
    val child2 = newScenario("child2", empty = true)
    val child3 = newScenario("child3", empty = false)
    val child4 = newScenario("child4", empty = true)
    val child5 = newScenario("child5", empty = false)

    val (rootScenarios, childrenScenarios) = SimulationParams.removeScenariosWithEmptyInjectionProfiles(
      List(nonEmptyRootScenario, emptyRootScenario),
      Map(
        nonEmptyRootScenario.name -> List(child1, child2),
        emptyRootScenario.name -> List(child3, child4),
        child1.name -> Nil,
        child2.name -> Nil,
        child3.name -> Nil,
        child4.name -> List(child5),
        child5.name -> Nil
      )
    )

    rootScenarios shouldBe List(nonEmptyRootScenario, child3, child5)
    childrenScenarios shouldBe Map(
      nonEmptyRootScenario.name -> List(child1),
      child1.name -> Nil,
      child3.name -> Nil,
      child5.name -> Nil
    )
  }
}
