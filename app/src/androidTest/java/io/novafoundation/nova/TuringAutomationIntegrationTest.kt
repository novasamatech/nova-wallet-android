package io.novafoundation.nova

import android.util.Log
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.AutomationAction
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.OptimalAutomationRequest
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.findChain
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigInteger

class TuringAutomationIntegrationTest : BaseIntegrationTest() {

    private val stakingApi = FeatureUtils.getFeature<StakingFeatureApi>(context, StakingFeatureApi::class.java)
    private val automationTasksRepository = stakingApi.turingAutomationRepository

    @Test
    fun calculateOptimalAutoCompounding(){
        runBlocking {
            val chain = chainRegistry.findTuringChain()
            val request = OptimalAutomationRequest(
                collator = "6AEG2WKRVvZteWWT3aMkk2ZE21FvURqiJkYpXimukub8Zb9C",
                amount = BigInteger("1000000000000")
            )

            val response = automationTasksRepository.calculateOptimalAutomation(chain.id, request)

            Log.d(LOG_TAG, response.toString())
        }
    }

    @Test
    fun calculateAutoCompoundExecutionFees(){
        runBlocking {
            val chain = chainRegistry.findTuringChain()
            val fees = automationTasksRepository.getTimeAutomationFees(chain.id, AutomationAction.AUTO_COMPOUND_DELEGATED_STAKE, executions = 1)

            Log.d(LOG_TAG, fees.toString())
        }
    }

    private suspend fun ChainRegistry.findTuringChain() =  findChain { it.name == "Turing" }!!
}
