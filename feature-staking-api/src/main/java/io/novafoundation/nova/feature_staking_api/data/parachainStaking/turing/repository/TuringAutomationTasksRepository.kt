package io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow


interface TuringAutomationTasksRepository {

    fun automationTasksFlow(chainId: ChainId, accountId: AccountId): Flow<List<TuringAutomationTask>>

    suspend fun calculateOptimalAutomation(chainId: ChainId, request: OptimalAutomationRequest): OptimalAutomationResponse
}
