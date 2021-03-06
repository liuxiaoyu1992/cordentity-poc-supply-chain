/*
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.luxoft.web.controllers

import com.luxoft.blockchainlab.corda.hyperledger.indy.data.state.IndyClaim
import com.luxoft.poc.supplychain.data.BusinessEntity
import com.luxoft.poc.supplychain.data.ChainOfAuthority
import com.luxoft.poc.supplychain.data.state.Package
import com.luxoft.poc.supplychain.flow.PackageWithdrawal
import com.luxoft.poc.supplychain.flow.medicine.AskNewPackage
import com.luxoft.web.components.IndyInitializer.Companion.issuerCert
import com.luxoft.web.components.IndyInitializer.Companion.treatmentCert
import com.luxoft.web.components.RPCComponent
import com.luxoft.web.data.AskForPackageRequest
import com.luxoft.web.data.FAILURE
import com.luxoft.web.data.Serial
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.loggerFor
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("api/sa")
@CrossOrigin
@Profile("sovrinagent")
class SovrinAgentController(rpc: RPCComponent) {
    private final val services = rpc.services
    private final val logger = loggerFor<SovrinAgentController>()

    @GetMapping("whoami")
    fun getWhoAmI(): Any {
        return services.nodeInfo().legalIdentities.first().name.organisation
    }


    @PostMapping("request/create")
    fun createPackageRequest(@RequestBody tc: AskForPackageRequest): Any? {

        return try {
            val chainOfAuthority = ChainOfAuthority()
                    .add(BusinessEntity.Treatment, treatmentCert)
                    .add(BusinessEntity.Manufacturer, issuerCert)
                    .add(BusinessEntity.Insuranse, treatmentCert)
                    .add(BusinessEntity.Goverment, issuerCert)

            val flowHandle = services.startFlowDynamic(AskNewPackage.Patient::class.java, chainOfAuthority)
            val result = flowHandle.returnValue.get()
            return result

        } catch (e: Exception) {
            logger.error("", e)
            FAILURE.plus("error" to e.message)
        }
    }


    @PostMapping("package/withdraw")
    fun receivePackage(@RequestBody request: Serial): Any? {

        return try {

            val flowHandle = services.startFlowDynamic(PackageWithdrawal.Owner::class.java, request.serial)
            flowHandle.returnValue.get()
            null

        } catch (e: Exception) {
            logger.error("", e)
            FAILURE.plus("error" to e.message)
        }
    }

    @GetMapping("package/list")
    fun getPackageRequests(): Any {
        return try {
            services.vaultQueryBy<Package>().states.map { it.state.data.info }

        } catch (e: Exception) {
            logger.error("", e)
            FAILURE.plus("error" to e.message)
        }
    }

    @GetMapping("claim/list")
    fun getClaims(): Any {
        return try {
            services.vaultQueryBy<IndyClaim>().states
                    .map { it.state.data.claimInfo.claim }
                    .map { SerializationUtils.anyToJSON(it) }

        } catch (e: Exception) {
            logger.error("", e)
            FAILURE.plus("error" to e.message)
        }
    }
}
