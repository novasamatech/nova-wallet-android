package io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface TuringAutomationTasksRepository {

    fun automationTasksFlow(chainId: ChainId, accountId: AccountId): Flow<List<TuringAutomationTask>>

    suspend fun calculateOptimalAutomation(chainId: ChainId, request: OptimalAutomationRequest): OptimalAutomationResponse

    suspend fun getTimeAutomationFees(chainId: ChainId, action: AutomationAction, executions: Int): Balance
}
