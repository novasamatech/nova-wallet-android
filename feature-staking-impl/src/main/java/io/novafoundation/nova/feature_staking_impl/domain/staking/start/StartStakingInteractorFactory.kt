package io.novafoundation.nova.feature_staking_impl.domain.staking.start

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.createStakingOption
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.era.StakingEraInteractorFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.ParachainNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.direct.ParachainStartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.direct.RelaychainStartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.nomination.NominationPoolStartStakingInteractor
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.group
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

class StartStakingInteractorFactory(
    private val stakingSharedComputation: StakingSharedComputation,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val stakingEraInteractorFactory: StakingEraInteractorFactory,
    private val parachainNetworkInfoInteractor: ParachainNetworkInfoInteractor,
    private val parachainStakingRewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
    private val nominationPoolGlobalsRepository: NominationPoolGlobalsRepository
) {

    suspend fun create(chain: Chain, asset: Chain.Asset, coroutineScope: CoroutineScope): CompoundStartStakingInteractor {
        val interactors = createInteractors(chain, asset, coroutineScope)
        val stakingEraInteractor = stakingEraInteractorFactory.create(chain)
        return RealCompoundStartStakingInteractor(walletRepository, accountRepository, interactors, stakingEraInteractor)
    }

    private suspend fun createInteractors(chain: Chain, asset: Chain.Asset, coroutineScope: CoroutineScope): List<StartStakingInteractor> {
        return asset.staking.mapNotNull {
            when (it.group()) {
                StakingTypeGroup.RELAYCHAIN -> createRelaychainStartStakingInteractor(coroutineScope)
                StakingTypeGroup.PARACHAIN -> createPararchainStartStakingInteractor(coroutineScope, chain, asset, it)
                StakingTypeGroup.NOMINATION_POOL -> createNominationPoolsStartStakingInteractor(coroutineScope)
                StakingTypeGroup.UNSUPPORTED -> null
            }
        }
    }

    private fun createRelaychainStartStakingInteractor(coroutineScope: CoroutineScope): StartStakingInteractor {
        return RelaychainStartStakingInteractor(
            stakingSharedComputation = stakingSharedComputation,
            accountRepository = accountRepository,
            walletRepository = walletRepository,
            coroutineScope = coroutineScope,
            stakingType = Chain.Asset.StakingType.RELAYCHAIN
        )
    }

    private suspend fun createPararchainStartStakingInteractor(
        coroutineScope: CoroutineScope,
        chain: Chain,
        asset: Chain.Asset,
        stakingType: Chain.Asset.StakingType
    ): StartStakingInteractor {
        return ParachainStartStakingInteractor(
            accountRepository = accountRepository,
            walletRepository = walletRepository,
            coroutineScope = coroutineScope,
            parachainNetworkInfoInteractor = parachainNetworkInfoInteractor,
            stakingType = Chain.Asset.StakingType.PARACHAIN,
            parachainStakingRewardCalculator = parachainStakingRewardCalculatorFactory.create(createStakingOption(chain, asset, stakingType))
        )
    }

    private fun createNominationPoolsStartStakingInteractor(coroutineScope: CoroutineScope): StartStakingInteractor {
        return NominationPoolStartStakingInteractor(
            accountRepository = accountRepository,
            walletRepository = walletRepository,
            coroutineScope = coroutineScope,
            stakingType = Chain.Asset.StakingType.NOMINATION_POOLS,
            nominationPoolGlobalsRepository = nominationPoolGlobalsRepository
        )
    }
}
