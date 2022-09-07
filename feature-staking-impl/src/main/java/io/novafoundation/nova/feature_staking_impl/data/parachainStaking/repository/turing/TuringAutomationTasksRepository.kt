package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.turing

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.automationTime
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class TuringAutomationTask(
    val id: String,
    val delegator: AccountId,
    val collator: AccountId,
    val accountMinimum: Balance,
    val frequency: BlockNumber
)

interface TuringAutomationTasksRepository {

    fun automationTasksFlow(chainId: ChainId, accountId: AccountId): Flow<List<TuringAutomationTask>>
}

class RealTuringAutomationTasksRepository(
    private val localStorageDataSource: StorageDataSource
): TuringAutomationTasksRepository {

    override fun automationTasksFlow(chainId: ChainId, accountId: AccountId): Flow<List<TuringAutomationTask>> {
        return localStorageDataSource.subscribe(chainId) {
            runtime.metadata.automationTime().storage("AccountTasks").observeByPrefix(
                accountId,
                keyExtractor = { (_: AccountId, taskId: ByteArray) -> taskId.toHexString() },
                binding = ::bindAutomationTasks
            ).map { it.values.toList() }
        }
    }

    private fun bindAutomationTasks(raw: Any?, taskId: String): TuringAutomationTask {
        println()

        return TuringAutomationTask(
            id = taskId,
            delegator = byteArrayOf(),
            collator = byteArrayOf(),
            accountMinimum = BigInteger.ONE,
            frequency = BigInteger.ONE
        )
    }
}
