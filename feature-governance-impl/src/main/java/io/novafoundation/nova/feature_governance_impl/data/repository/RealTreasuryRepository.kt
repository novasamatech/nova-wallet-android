package io.novafoundation.nova.feature_governance_impl.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStructOrNull
import io.novafoundation.nova.common.utils.treasury
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TreasuryProposal
import io.novafoundation.nova.feature_governance_api.data.repository.TreasuryRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.metadata.storage

class RealTreasuryRepository(
    private val remoteSource: StorageDataSource
) : TreasuryRepository {

    override suspend fun getTreasuryProposal(chainId: ChainId, id: TreasuryProposal.Id): TreasuryProposal? {
        return remoteSource.query(chainId) {
            runtime.metadata.treasury().storage("Proposals").query(
                id.value,
                binding = { bindProposal(id, it) }
            )
        }
    }

    private fun bindProposal(id: TreasuryProposal.Id, decoded: Any?): TreasuryProposal? {
        val asStruct = decoded.castToStructOrNull() ?: return null

        return TreasuryProposal(
            id = id,
            proposer = bindAccountId(asStruct["proposer"]),
            amount = bindNumber(asStruct["value"]),
            beneficiary = bindAccountId(asStruct["beneficiary"]),
            bond = bindNumber(asStruct["bond"])
        )
    }
}
