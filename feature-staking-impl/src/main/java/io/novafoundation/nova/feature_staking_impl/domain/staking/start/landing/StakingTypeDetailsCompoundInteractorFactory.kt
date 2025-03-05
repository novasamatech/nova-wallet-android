package io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.MultiStakingOptionIds
import io.novafoundation.nova.feature_staking_impl.data.createStakingOption
import io.novafoundation.nova.feature_staking_impl.domain.era.StakingEraInteractorFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetailsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.direct.StakingTypeDetailsInteractorFactory
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.group
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset

class StakingTypeDetailsCompoundInteractorFactory(
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val stakingEraInteractorFactory: StakingEraInteractorFactory,
    private val stakingTypeDetailsInteractorFactories: Map<StakingTypeGroup, StakingTypeDetailsInteractorFactory>,
    private val chainRegistry: ChainRegistry
) {

    suspend fun create(
        multiStakingOptionIds: MultiStakingOptionIds,
        computationalScope: ComputationalScope
    ): StakingTypeDetailsCompoundInteractor {
        val (chain, chainAsset) = chainRegistry.chainWithAsset(multiStakingOptionIds.chainId, multiStakingOptionIds.chainAssetId)
        val interactors = createInteractors(chain, chainAsset, multiStakingOptionIds.stakingTypes, computationalScope)
        val stakingEraInteractor = stakingEraInteractorFactory.create(chain, chainAsset, computationalScope)

        return RealStakingTypeDetailsCompoundInteractor(
            chain = chain,
            chainAsset = chainAsset,
            walletRepository = walletRepository,
            accountRepository = accountRepository,
            interactors = interactors,
            stakingEraInteractor = stakingEraInteractor
        )
    }

    private suspend fun createInteractors(
        chain: Chain,
        asset: Chain.Asset,
        stakingTypes: List<Chain.Asset.StakingType>,
        computationalScope: ComputationalScope
    ): List<StakingTypeDetailsInteractor> {
        return stakingTypes.mapNotNull { stakingType ->
            val stakingOption = createStakingOption(chain, asset, stakingType)

            val supportedInteractorFactory = stakingTypeDetailsInteractorFactories[stakingType.group()]
            supportedInteractorFactory?.create(stakingOption, computationalScope)
        }
    }
}
