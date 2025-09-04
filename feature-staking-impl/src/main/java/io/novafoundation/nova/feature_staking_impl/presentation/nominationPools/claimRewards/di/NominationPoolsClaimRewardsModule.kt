package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.claimRewards.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolMembersRepository
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.claimRewards.NominationPoolsClaimRewardsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.claimRewards.RealNominationPoolsClaimRewardsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.claimRewards.validations.NominationPoolsClaimRewardsValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.claimRewards.validations.nominationPoolsClaimRewards
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.delegatedStake.DelegatedStakeMigrationUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.StakingTypesConflictValidationFactory
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.claimRewards.NominationPoolsClaimRewardsViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class NominationPoolsClaimRewardsModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        poolMemberUseCase: NominationPoolMemberUseCase,
        poolMembersRepository: NominationPoolMembersRepository,
        stakingSharedState: StakingSharedState,
        extrinsicService: ExtrinsicService,
        migrationUseCase: DelegatedStakeMigrationUseCase
    ): NominationPoolsClaimRewardsInteractor = RealNominationPoolsClaimRewardsInteractor(
        poolMemberUseCase = poolMemberUseCase,
        poolMembersRepository = poolMembersRepository,
        stakingSharedState = stakingSharedState,
        extrinsicService = extrinsicService,
        migrationUseCase = migrationUseCase
    )

    @Provides
    @ScreenScope
    fun provideValidationSystem(
        enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory,
        stakingTypesConflictValidationFactory: StakingTypesConflictValidationFactory
    ): NominationPoolsClaimRewardsValidationSystem {
        return ValidationSystem.nominationPoolsClaimRewards(enoughTotalToStayAboveEDValidationFactory, stakingTypesConflictValidationFactory)
    }

    @Provides
    @IntoMap
    @ViewModelKey(NominationPoolsClaimRewardsViewModel::class)
    fun provideViewModel(
        router: NominationPoolsRouter,
        interactor: NominationPoolsClaimRewardsInteractor,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        validationSystem: NominationPoolsClaimRewardsValidationSystem,
        stakingSharedState: StakingSharedState,
        externalActions: ExternalActions.Presentation,
        selectedAccountUseCase: SelectedAccountUseCase,
        walletUiUseCase: WalletUiUseCase,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        assetUseCase: AssetUseCase,
    ): ViewModel {
        return NominationPoolsClaimRewardsViewModel(
            router = router,
            interactor = interactor,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            validationSystem = validationSystem,
            stakingSharedState = stakingSharedState,
            externalActions = externalActions,
            selectedAccountUseCase = selectedAccountUseCase,
            walletUiUseCase = walletUiUseCase,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            assetUseCase = assetUseCase
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): NominationPoolsClaimRewardsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(NominationPoolsClaimRewardsViewModel::class.java)
    }
}
