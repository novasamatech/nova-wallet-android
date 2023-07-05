package io.novafoundation.nova.feature_staking_impl.domain.staking.start

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.era.StakingEraInteractorFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.direct.ParachainStartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.direct.RelaychainStartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.nomination.NominationPoolStartStakingInteractor
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.group
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

class StartStakingInteractorFactory(
    private val stakingSharedState: StakingSharedState,
    private val stakingSharedComputation: StakingSharedComputation,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val stakingEraInteractorFactory: StakingEraInteractorFactory
) {

    fun create(chain: Chain, asset: Chain.Asset, coroutineScope: CoroutineScope): CompoundStartStakingInteractor {
        val interactors = createInteractors(asset, coroutineScope)
        val stakingEraInteractor = stakingEraInteractorFactory.create(chain)
        return CompoundStartStakingInteractor(stakingSharedState, walletRepository, accountRepository, interactors, stakingEraInteractor)
    }

    private fun createInteractors(asset: Chain.Asset, coroutineScope: CoroutineScope): List<StartStakingInteractor> {
        return asset.staking.mapNotNull {
            when (it.group()) {
                StakingTypeGroup.RELAYCHAIN -> createRelaychainStartStakingInteractor(coroutineScope)
                StakingTypeGroup.PARACHAIN -> createPararchainStartStakingInteractor(coroutineScope)
                StakingTypeGroup.NOMINATION_POOL -> createNominationPoolsStartStakingInteractor()
                StakingTypeGroup.UNSUPPORTED -> null
            }
        }
    }

    private fun createRelaychainStartStakingInteractor(coroutineScope: CoroutineScope): StartStakingInteractor {
        return RelaychainStartStakingInteractor(
            stakingSharedState = stakingSharedState,
            stakingSharedComputation = stakingSharedComputation,
            coroutineScope = coroutineScope
        )
    }

    private fun createPararchainStartStakingInteractor(coroutineScope: CoroutineScope): StartStakingInteractor {
        return ParachainStartStakingInteractor(
            stakingSharedState = stakingSharedState,
            stakingSharedComputation = stakingSharedComputation,
            coroutineScope = coroutineScope
        )
    }

    private fun createNominationPoolsStartStakingInteractor(): StartStakingInteractor {
        return NominationPoolStartStakingInteractor()
    }
}
