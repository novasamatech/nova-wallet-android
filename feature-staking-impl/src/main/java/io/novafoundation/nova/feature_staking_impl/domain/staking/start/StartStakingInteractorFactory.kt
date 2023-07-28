package io.novafoundation.nova.feature_staking_impl.domain.staking.start

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.createStakingOption
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
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.CoroutineScope

class StartStakingInteractorFactory(
    private val stakingSharedComputation: StakingSharedComputation,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val stakingEraInteractorFactory: StakingEraInteractorFactory,
    private val parachainNetworkInfoInteractor: ParachainNetworkInfoInteractor,
    private val parachainStakingRewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
    private val chainRegistry: ChainRegistry
) {

    suspend fun create(chainId: ChainId, assetId: Int, coroutineScope: CoroutineScope): CompoundStartStakingInteractor {
        val chain = chainRegistry.getChain(chainId)
        val chainAsset = chain.assetsById.getValue(assetId)
        val interactors = createInteractors(chain, chainAsset, coroutineScope)
        val stakingEraInteractor = stakingEraInteractorFactory.create(chainAsset)
        return RealCompoundStartStakingInteractor(chain, chainAsset, walletRepository, accountRepository, interactors, stakingEraInteractor)
    }

    private suspend fun createInteractors(chain: Chain, asset: Chain.Asset, coroutineScope: CoroutineScope): List<StartStakingInteractor> {
        return asset.staking.mapNotNull { stakingType ->
            when (stakingType.group()) {
                StakingTypeGroup.RELAYCHAIN -> createRelaychainStartStakingInteractor(coroutineScope, stakingType)
                StakingTypeGroup.PARACHAIN -> createPararchainStartStakingInteractor(chain, asset, stakingType)
                StakingTypeGroup.NOMINATION_POOL -> createNominationPoolsStartStakingInteractor()
                StakingTypeGroup.UNSUPPORTED -> null
            }
        }
    }

    private fun createRelaychainStartStakingInteractor(coroutineScope: CoroutineScope, stakingType: Chain.Asset.StakingType): StartStakingInteractor {
        return RelaychainStartStakingInteractor(
            stakingSharedComputation = stakingSharedComputation,
            coroutineScope = coroutineScope,
            stakingType = stakingType
        )
    }

    private suspend fun createPararchainStartStakingInteractor(
        chain: Chain,
        asset: Chain.Asset,
        stakingType: Chain.Asset.StakingType
    ): StartStakingInteractor {
        return ParachainStartStakingInteractor(
            parachainNetworkInfoInteractor = parachainNetworkInfoInteractor,
            parachainStakingRewardCalculator = parachainStakingRewardCalculatorFactory.create(createStakingOption(chain, asset, stakingType))
        )
    }

    private fun createNominationPoolsStartStakingInteractor(): StartStakingInteractor {
        return NominationPoolStartStakingInteractor()
    }
}
