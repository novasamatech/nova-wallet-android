package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindNullableAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.constant
import io.novafoundation.nova.common.utils.electionProviderMultiPhaseOrNull
import io.novafoundation.nova.common.utils.numberConstantOrNull
import io.novafoundation.nova.common.utils.voterListOrNull
import io.novafoundation.nova.feature_staking_impl.domain.bagList.BagListLocator
import io.novafoundation.nova.feature_staking_impl.domain.model.BagListNode
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.network.binding.collectionOf
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.math.BigInteger

interface BagListRepository {

    suspend fun bagThresholds(chainId: ChainId): List<BagListNode.Score>?

    suspend fun bagListSize(chainId: ChainId): BigInteger?

    suspend fun maxElectingVotes(chainId: ChainId): BigInteger?

    fun listNodeFlow(stash: AccountId, chainId: ChainId): Flow<BagListNode?>
}

suspend fun BagListRepository.bagListLocatorOrNull(chainId: ChainId): BagListLocator? = bagThresholds(chainId)?.let(::BagListLocator)
suspend fun BagListRepository.bagListLocatorOrThrow(chainId: ChainId): BagListLocator = requireNotNull(bagListLocatorOrNull(chainId))

class LocalBagListRepository(
    private val localStorage: StorageDataSource
) : BagListRepository {

    private val knownMaxElectingVoters = mapOf(
        Chain.Geneses.KUSAMA to 12500,
        Chain.Geneses.POLKADOT to 22500,
    )

    override suspend fun bagThresholds(chainId: ChainId): List<BagListNode.Score>? {
        return localStorage.query(chainId) {
            runtime.metadata.voterListOrNull()?.constant("BagThresholds")?.getAs(collectionOf(::score))
        }
    }

    override suspend fun bagListSize(chainId: ChainId): BigInteger? {
        return localStorage.query(chainId) {
            runtime.metadata.voterListOrNull()?.storage("CounterForListNodes")?.query(binding = ::bindNumber)
        }
    }

    override suspend fun maxElectingVotes(chainId: ChainId): BigInteger? {
        return localStorage.query(chainId) {
            runtime.metadata.electionProviderMultiPhaseOrNull()?.numberConstantOrNull("MaxElectingVoters", runtime)
                ?: knownMaxElectingVoters[chainId]?.toBigInteger()
        }
    }

    override fun listNodeFlow(stash: AccountId, chainId: ChainId): Flow<BagListNode?> {
        return localStorage.subscribe(chainId) {
            runtime.metadata.voterListOrNull()?.storage("ListNodes")?.observe(stash, binding = ::bindBagListNode)
                ?: flowOf(null)
        }
    }

    private fun bindBagListNode(decoded: Any?): BagListNode? = runCatching {
        val nodeStruct = decoded.castToStruct()

        return BagListNode(
            id = bindAccountId(nodeStruct["id"]),
            previous = bindNullableAccountId(nodeStruct["prev"]),
            next = bindNullableAccountId(nodeStruct["next"]),
            bagUpper = score(nodeStruct["bagUpper"]),
            score = score(nodeStruct["score"]),
        )
    }.getOrNull()

    private fun score(decoded: Any?): BagListNode.Score = BagListNode.Score(bindNumber(decoded))
}
