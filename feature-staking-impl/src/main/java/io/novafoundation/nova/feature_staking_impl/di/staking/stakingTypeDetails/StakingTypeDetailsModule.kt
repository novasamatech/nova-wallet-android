package io.novafoundation.nova.feature_staking_impl.di.staking.stakingTypeDetails

import dagger.MapKey
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.era.StakingEraInteractorFactory
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.ParachainNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.NominationPoolsAvailableBalanceResolver
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.direct.ParachainStakingTypeDetailsInteractorFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.direct.RelaychainStakingTypeDetailsInteractorFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.direct.StakingTypeDetailsInteractorFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.pools.PoolStakingTypeDetailsInteractorFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.StakingTypeDetailsCompoundInteractorFactory
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@MapKey
annotation class StakingTypeDetailsKey(val group: StakingTypeGroup)

@Module
class StakingTypeDetailsModule {

    @Provides
    @FeatureScope
    fun providePoolStakingTypeDetailsInteractorFactory(
        nominationPoolSharedComputation: NominationPoolSharedComputation,
        poolsAvailableBalanceResolver: NominationPoolsAvailableBalanceResolver
    ): PoolStakingTypeDetailsInteractorFactory {
        return PoolStakingTypeDetailsInteractorFactory(
            nominationPoolSharedComputation,
            poolsAvailableBalanceResolver
        )
    }

    @Provides
    @FeatureScope
    fun provideRelaychainStakingTypeDetailsInteractorFactory(
        stakingSharedComputation: StakingSharedComputation
    ): RelaychainStakingTypeDetailsInteractorFactory {
        return RelaychainStakingTypeDetailsInteractorFactory(stakingSharedComputation)
    }

    @Provides
    @FeatureScope
    fun provideParachainStakingTypeDetailsInteractorFactory(
        parachainNetworkInfoInteractor: ParachainNetworkInfoInteractor,
        parachainStakingRewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
    ): ParachainStakingTypeDetailsInteractorFactory {
        return ParachainStakingTypeDetailsInteractorFactory(
            parachainNetworkInfoInteractor,
            parachainStakingRewardCalculatorFactory
        )
    }

    @Provides
    @FeatureScope
    @IntoMap
    @StakingTypeDetailsKey(StakingTypeGroup.NOMINATION_POOL)
    fun provideAbstractPoolStakingTypeDetailsInteractorFactory(
        stakingTypeDetailsInteractorFactory: PoolStakingTypeDetailsInteractorFactory
    ): StakingTypeDetailsInteractorFactory {
        return stakingTypeDetailsInteractorFactory
    }

    @Provides
    @FeatureScope
    @IntoMap
    @StakingTypeDetailsKey(StakingTypeGroup.RELAYCHAIN)
    fun provideAbstractRelaychainStakingTypeDetailsInteractorFactory(
        stakingTypeDetailsInteractorFactory: RelaychainStakingTypeDetailsInteractorFactory
    ): StakingTypeDetailsInteractorFactory {
        return stakingTypeDetailsInteractorFactory
    }

    @Provides
    @FeatureScope
    @IntoMap
    @StakingTypeDetailsKey(StakingTypeGroup.PARACHAIN)
    fun provideAbstractParachainStakingTypeDetailsInteractorFactory(
        stakingTypeDetailsInteractorFactory: ParachainStakingTypeDetailsInteractorFactory
    ): StakingTypeDetailsInteractorFactory {
        return stakingTypeDetailsInteractorFactory
    }

    @Provides
    fun provideStartStakingInteractorFactory(
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        stakingEraInteractorFactory: StakingEraInteractorFactory,
        chainRegistry: ChainRegistry,
        factories: Map<StakingTypeGroup, @JvmSuppressWildcards StakingTypeDetailsInteractorFactory>,
    ): StakingTypeDetailsCompoundInteractorFactory {
        return StakingTypeDetailsCompoundInteractorFactory(
            walletRepository = walletRepository,
            accountRepository = accountRepository,
            stakingEraInteractorFactory = stakingEraInteractorFactory,
            stakingTypeDetailsInteractorFactories = factories,
            chainRegistry = chainRegistry
        )
    }
}
