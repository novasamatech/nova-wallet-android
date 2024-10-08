package io.novafoundation.nova.feature_governance_impl.data.repository.tindergov

import io.novafoundation.nova.core_db.dao.TinderGovDao
import io.novafoundation.nova.core_db.model.TinderGovVotingPowerLocal
import io.novafoundation.nova.feature_governance_api.data.model.VotingPower
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface TinderGovVotingPowerRepository {

    suspend fun setVotingPower(votingPower: VotingPower)

    suspend fun getVotingPower(metaId: Long, chainId: ChainId): VotingPower?
}

class RealTinderGovVotingPowerRepository(
    private val tinderGovDao: TinderGovDao
) : TinderGovVotingPowerRepository {

    override suspend fun setVotingPower(votingPower: VotingPower) {
        val local = TinderGovVotingPowerLocal(
            metaId = votingPower.metaId,
            chainId = votingPower.chainId,
            amount = votingPower.amount,
            conviction = votingPower.conviction.toLocal()
        )

        tinderGovDao.setVotingPower(local)
    }

    override suspend fun getVotingPower(metaId: Long, chainId: ChainId): VotingPower? {
        val local = tinderGovDao.getVotingPower(metaId, chainId) ?: return null
        return VotingPower(
            metaId = local.metaId,
            chainId = local.chainId,
            amount = local.amount,
            conviction = local.conviction.toDomain()
        )
    }
}
