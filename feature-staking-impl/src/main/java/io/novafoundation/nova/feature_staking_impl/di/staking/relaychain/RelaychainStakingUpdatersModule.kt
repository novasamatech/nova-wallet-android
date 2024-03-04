package io.novafoundation.nova.feature_staking_impl.di.staking.relaychain

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core_db.dao.AccountStakingDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.AccountNominationsUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.AccountRewardDestinationUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.AccountValidatorPrefsUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.ActiveEraUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.BagListNodeUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.CounterForListNodesUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.CounterForNominatorsUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.CurrentEraUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.HistoryDepthUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.MaxNominatorsUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.MinBondUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.ParachainsUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.ProxiesUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingLedgerUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingUpdaters
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.ValidatorExposureUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.controller.AccountControllerBalanceUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.historical.HistoricalTotalValidatorRewardUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.historical.HistoricalUpdateMediator
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.historical.HistoricalValidatorRewardPointsUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.scope.AccountStakingScope
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.scope.ActiveEraScope
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.CurrentEpochIndexUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.CurrentSessionIndexUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.CurrentSlotUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.EraStartSessionIndexUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session.GenesisSlotUpdater
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.ElectionsSessionRegistry
import io.novafoundation.nova.feature_staking_impl.di.staking.DefaultBulkRetriever
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class RelaychainStakingUpdatersModule {

    @Provides
    @FeatureScope
    fun provideAccountStakingScope(
        accountRepository: AccountRepository,
        accountStakingDao: AccountStakingDao,
        sharedState: StakingSharedState,
    ) = AccountStakingScope(
        accountRepository,
        accountStakingDao,
        sharedState
    )

    @Provides
    @FeatureScope
    fun provideActiveEraScope(
        stakingSharedComputation: StakingSharedComputation,
        stakingSharedState: StakingSharedState
    ) = ActiveEraScope(
        stakingSharedComputation = stakingSharedComputation,
        stakingSharedState = stakingSharedState
    )

    @Provides
    @FeatureScope
    fun provideActiveEraUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
    ) = ActiveEraUpdater(
        sharedState,
        chainRegistry,
        storageCache
    )

    @Provides
    @FeatureScope
    fun provideElectedNominatorsUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        @DefaultBulkRetriever bulkRetriever: BulkRetriever,
        storageCache: StorageCache,
        scope: ActiveEraScope
    ) = ValidatorExposureUpdater(
        bulkRetriever,
        sharedState,
        chainRegistry,
        storageCache,
        scope
    )

    @Provides
    @FeatureScope
    fun provideCurrentEraUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
    ) = CurrentEraUpdater(
        sharedState,
        chainRegistry,
        storageCache
    )

    @Provides
    @FeatureScope
    fun provideStakingLedgerUpdater(
        stakingRepository: StakingRepository,
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        accountStakingDao: AccountStakingDao,
        assetCache: AssetCache,
        storageCache: StorageCache,
        accountUpdateScope: AccountUpdateScope,
    ): StakingLedgerUpdater {
        return StakingLedgerUpdater(
            stakingRepository,
            sharedState,
            chainRegistry,
            accountStakingDao,
            storageCache,
            assetCache,
            accountUpdateScope
        )
    }

    @Provides
    @FeatureScope
    fun provideAccountValidatorPrefsUpdater(
        storageCache: StorageCache,
        scope: AccountStakingScope,
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = AccountValidatorPrefsUpdater(
        scope,
        storageCache,
        sharedState,
        chainRegistry,
    )

    @Provides
    @FeatureScope
    fun provideAccountNominationsUpdater(
        storageCache: StorageCache,
        scope: AccountStakingScope,
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = AccountNominationsUpdater(
        scope,
        storageCache,
        sharedState,
        chainRegistry,
    )

    @Provides
    @FeatureScope
    fun provideAccountRewardDestinationUpdater(
        storageCache: StorageCache,
        scope: AccountStakingScope,
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = AccountRewardDestinationUpdater(
        scope,
        storageCache,
        sharedState,
        chainRegistry,
    )

    @Provides
    @FeatureScope
    fun provideHistoryDepthUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
    ) = HistoryDepthUpdater(
        sharedState,
        chainRegistry,
        storageCache
    )

    @Provides
    @FeatureScope
    fun provideHistoricalMediator(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
        activeEraScope: ActiveEraScope,
        preferences: Preferences
    ) = HistoricalUpdateMediator(
        historicalUpdaters = listOf(
            HistoricalTotalValidatorRewardUpdater(),
            HistoricalValidatorRewardPointsUpdater(),
        ),
        stakingSharedState = sharedState,
        chainRegistry = chainRegistry,
        storageCache = storageCache,
        scope = activeEraScope,
        preferences = preferences
    )

    @Provides
    @FeatureScope
    fun provideEraStartSessionIndexUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
        activeEraScope: ActiveEraScope,
    ) = EraStartSessionIndexUpdater(
        activeEraScope = activeEraScope,
        storageCache = storageCache,
        stakingSharedState = sharedState,
        chainRegistry = chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideCurrentSessionIndexUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
    ) = CurrentSessionIndexUpdater(
        sharedState,
        chainRegistry,
        storageCache
    )

    @Provides
    @FeatureScope
    fun provideCurrentSlotUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
        electionsSessionRegistry: ElectionsSessionRegistry,
    ) = CurrentSlotUpdater(
        electionsSessionRegistry = electionsSessionRegistry,
        stakingSharedState = sharedState,
        chainRegistry = chainRegistry,
        storageCache = storageCache
    )

    @Provides
    @FeatureScope
    fun provideGenesisSlotUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
        electionsSessionRegistry: ElectionsSessionRegistry,
    ) = GenesisSlotUpdater(
        electionsSessionRegistry = electionsSessionRegistry,
        stakingSharedState = sharedState,
        chainRegistry = chainRegistry,
        storageCache = storageCache
    )

    @Provides
    @FeatureScope
    fun provideCurrentEpochIndexUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
        electionsSessionRegistry: ElectionsSessionRegistry,
    ) = CurrentEpochIndexUpdater(
        electionsSessionRegistry = electionsSessionRegistry,
        stakingSharedState = sharedState,
        chainRegistry = chainRegistry,
        storageCache = storageCache
    )

    @Provides
    @FeatureScope
    fun provideAccountControllerBalanceUpdater(
        assetCache: AssetCache,
        scope: AccountStakingScope,
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = AccountControllerBalanceUpdater(
        scope,
        sharedState,
        chainRegistry,
        assetCache
    )

    @Provides
    @FeatureScope
    fun provideMinBondUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
    ) = MinBondUpdater(
        sharedState,
        chainRegistry,
        storageCache
    )

    @Provides
    @FeatureScope
    fun provideMaxNominatorsUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
    ) = MaxNominatorsUpdater(
        storageCache,
        sharedState,
        chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideCounterForNominatorsUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
    ) = CounterForNominatorsUpdater(
        sharedState,
        chainRegistry,
        storageCache
    )

    @Provides
    @FeatureScope
    fun provideBagListNodeUpdater(
        storageCache: StorageCache,
        scope: AccountStakingScope,
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = BagListNodeUpdater(
        scope,
        storageCache,
        sharedState,
        chainRegistry,
    )

    @Provides
    @FeatureScope
    fun provideCounterForListNodesUpdater(
        storageCache: StorageCache,
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = CounterForListNodesUpdater(
        storageCache,
        sharedState,
        chainRegistry,
    )

    @Provides
    @FeatureScope
    fun provideParasUpdater(
        storageCache: StorageCache,
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = ParachainsUpdater(
        storageCache,
        sharedState,
        chainRegistry,
    )

    @Provides
    @FeatureScope
    fun provideProxiesUpdater(
        storageCache: StorageCache,
        scope: AccountStakingScope,
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = ProxiesUpdater(
        scope,
        sharedState,
        chainRegistry,
        storageCache,
    )

    @Provides
    @Relaychain
    @FeatureScope
    fun provideRelaychainStakingUpdaters(
        activeEraUpdater: ActiveEraUpdater,
        validatorExposureUpdater: ValidatorExposureUpdater,
        currentEraUpdater: CurrentEraUpdater,
        stakingLedgerUpdater: StakingLedgerUpdater,
        accountValidatorPrefsUpdater: AccountValidatorPrefsUpdater,
        accountNominationsUpdater: AccountNominationsUpdater,
        rewardDestinationUpdater: AccountRewardDestinationUpdater,
        historyDepthUpdater: HistoryDepthUpdater,
        historicalUpdateMediator: HistoricalUpdateMediator,
        accountControllerBalanceUpdater: AccountControllerBalanceUpdater,
        minBondUpdater: MinBondUpdater,
        maxNominatorsUpdater: MaxNominatorsUpdater,
        counterForNominatorsUpdater: CounterForNominatorsUpdater,
        bagListNodeUpdater: BagListNodeUpdater,
        counterForListNodesUpdater: CounterForListNodesUpdater,
        parachainsUpdater: ParachainsUpdater,
        currentEpochIndexUpdater: CurrentEpochIndexUpdater,
        currentSlotUpdater: CurrentSlotUpdater,
        genesisSlotUpdater: GenesisSlotUpdater,
        currentSessionIndexUpdater: CurrentSessionIndexUpdater,
        eraStartSessionIndexUpdater: EraStartSessionIndexUpdater,
        proxiesUpdater: ProxiesUpdater
    ) = StakingUpdaters.Group(
        activeEraUpdater,
        validatorExposureUpdater,
        currentEraUpdater,
        stakingLedgerUpdater,
        accountValidatorPrefsUpdater,
        accountNominationsUpdater,
        rewardDestinationUpdater,
        historyDepthUpdater,
        historicalUpdateMediator,
        accountControllerBalanceUpdater,
        minBondUpdater,
        maxNominatorsUpdater,
        counterForNominatorsUpdater,
        bagListNodeUpdater,
        counterForListNodesUpdater,
        parachainsUpdater,
        currentEpochIndexUpdater,
        currentSlotUpdater,
        genesisSlotUpdater,
        currentSessionIndexUpdater,
        eraStartSessionIndexUpdater,
        proxiesUpdater
    )
}
