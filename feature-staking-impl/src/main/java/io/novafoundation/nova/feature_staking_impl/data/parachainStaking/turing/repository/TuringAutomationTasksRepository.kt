package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.turing.repository

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.common.utils.automationTime
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.AutomationAction
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.OptimalAutomationRequest
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.OptimalAutomationResponse
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.TuringAutomationTask
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.TuringAutomationTasksRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.turing.network.rpc.TuringAutomationRpcApi
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RealTuringAutomationTasksRepository(
    private val localStorageDataSource: StorageDataSource,
    private val turingAutomationRpcApi: TuringAutomationRpcApi,
) : TuringAutomationTasksRepository {

    override fun automationTasksFlow(chainId: ChainId, accountId: AccountId): Flow<List<TuringAutomationTask>> {
        return localStorageDataSource.subscribe(chainId) {
            runtime.metadata.automationTime().storage("AccountTasks").observeByPrefix(
                accountId,
                keyExtractor = { (_: AccountId, taskId: ByteArray) -> taskId.toHexString() },
                binding = ::bindAutomationTasks
            ).map { it.values.filterNotNull() }
        }
    }

    override suspend fun calculateOptimalAutomation(chainId: ChainId, request: OptimalAutomationRequest): OptimalAutomationResponse {
        return turingAutomationRpcApi.calculateOptimalAutomation(chainId, request)
    }

    override suspend fun getTimeAutomationFees(chainId: ChainId, action: AutomationAction, executions: Int): Balance {
        return turingAutomationRpcApi.getTimeAutomationFees(chainId, action, executions)
    }

    private fun bindAutomationTasks(raw: Any?, taskId: String): TuringAutomationTask? = runCatching {
        val struct = raw.castToStruct()
        val action = struct.get<DictEnum.Entry<*>>("action") ?: incompatible()
        val actionType = action.name

        if (actionType != "AutoCompoundDelegatedStake") return null

        val schedule = struct.get<DictEnum.Entry<*>>("schedule") ?: incompatible()
        val actionValue = action.value.castToStruct()

        return TuringAutomationTask(
            id = taskId,
            delegator = bindAccountId(actionValue["delegator"]),
            collator = bindAccountId(actionValue["collator"]),
            accountMinimum = bindNumber(actionValue["account_minimum"]),
            schedule = bindSchedule(schedule)
        )
    }.getOrNull()

    private fun bindSchedule(schedule: DictEnum.Entry<*>): TuringAutomationTask.Schedule = runCatching {
        if (schedule.name == "Recurring") {
            val recurring = schedule.value.castToStruct().get<BigInteger>("frequency") ?: incompatible()
            return TuringAutomationTask.Schedule.Recurring(recurring)
        }

        return TuringAutomationTask.Schedule.Unknown
    }.getOrNull() ?: TuringAutomationTask.Schedule.Unknown
}
