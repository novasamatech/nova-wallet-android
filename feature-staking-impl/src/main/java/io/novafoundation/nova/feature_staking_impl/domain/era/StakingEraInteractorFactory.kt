package io.novafoundation.nova.feature_staking_impl.domain.era

import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.runtime.ext.isParachain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource

class StakingEraInteractorFactory(
    private val roundDurationEstimator: RoundDurationEstimator,
    private val currentRoundRepository: CurrentRoundRepository,
    private val stakingInteractor: StakingInteractor,
    private val storageDataSource: StorageDataSource
) {

    fun create(chain: Chain): StakingEraInteractor {
        return if (chain.isParachain) {
            ParachainStakingEraInteractor(
                roundDurationEstimator,
                currentRoundRepository,
                storageDataSource
            )
        } else {
            RelaychainStakingEraInteractor(stakingInteractor)
        }
    }
}
