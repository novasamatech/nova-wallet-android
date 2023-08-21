package io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.MultiStakingOptionIds
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.createStakingOption
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.era.StakingEraInteractorFactory
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.ParachainNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.direct.ParachainStartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.direct.RelaychainStartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.nomination.NominationPoolStartStakingInteractor
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.group
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.CoroutineScope

class StartStakingInteractorFactory(
    private val stakingSharedComputation: StakingSharedComputation,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val stakingEraInteractorFactory: StakingEraInteractorFactory,
    private val parachainNetworkInfoInteractor: ParachainNetworkInfoInteractor,
    private val parachainStakingRewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
    private val nominationPoolGlobalsRepository: NominationPoolGlobalsRepository,
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val chainRegistry: ChainRegistry,
) {

    suspend fun create(
        multiStakingOptionIds: MultiStakingOptionIds,
        coroutineScope: CoroutineScope
    ): CompoundStartStakingInteractor {
        val (chain, chainAsset) = chainRegistry.chainWithAsset(multiStakingOptionIds.chainId, multiStakingOptionIds.chainAssetId)
        val interactors = createInteractors(chain, chainAsset, multiStakingOptionIds.stakingTypes, coroutineScope)
        val stakingEraInteractor = stakingEraInteractorFactory.create(chain, chainAsset, coroutineScope)

        return RealCompoundStartStakingInteractor(
            chain = chain,
            chainAsset = chainAsset,
            walletRepository = walletRepository,
            accountRepository = accountRepository,
            interactors = interactors,
            stakingEraInteractor = stakingEraInteractor,
        )
    }

    private suspend fun createInteractors(
        chain: Chain,
        asset: Chain.Asset,
        stakingTypes: List<Chain.Asset.StakingType>,
        coroutineScope: CoroutineScope
    ): List<StartStakingInteractor> {
        return stakingTypes.mapNotNull { stakingType ->
            val stakingOption = createStakingOption(chain, asset, stakingType)

            when (stakingType.group()) {
                StakingTypeGroup.RELAYCHAIN -> createRelaychainStartStakingInteractor(coroutineScope, stakingOption)
                StakingTypeGroup.PARACHAIN -> createParachainStartStakingInteractor(stakingOption)
                StakingTypeGroup.NOMINATION_POOL -> createNominationPoolsStartStakingInteractor(coroutineScope, stakingOption)
                StakingTypeGroup.UNSUPPORTED -> null
            }
        }
    }

    private fun createRelaychainStartStakingInteractor(coroutineScope: CoroutineScope, stakingOption: StakingOption): StartStakingInteractor {
        return RelaychainStartStakingInteractor(
            stakingSharedComputation = stakingSharedComputation,
            coroutineScope = coroutineScope,
            stakingOption = stakingOption
        )
    }

    private suspend fun createParachainStartStakingInteractor(
        stakingOption: StakingOption
    ): StartStakingInteractor {
        return ParachainStartStakingInteractor(
            parachainNetworkInfoInteractor = parachainNetworkInfoInteractor,
            parachainStakingRewardCalculator = parachainStakingRewardCalculatorFactory.create(stakingOption),
            stakingOption = stakingOption
        )
    }

    private fun createNominationPoolsStartStakingInteractor(coroutineScope: CoroutineScope, stakingOption: StakingOption): StartStakingInteractor {
        return NominationPoolStartStakingInteractor(
            stakingOption = stakingOption,
            scope = coroutineScope,
            nominationPoolSharedComputation = nominationPoolSharedComputation
        )
    }
}
