package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.redeem.di

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
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.redeem.NominationPoolsRedeemInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.redeem.RealNominationPoolsRedeemInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.redeem.validations.NominationPoolsRedeemValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.redeem.validations.nominationPoolsRedeem
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.redeem.NominationPoolsRedeemViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class NominationPoolsRedeemModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        extrinsicService: ExtrinsicService,
        stakingRepository: StakingRepository,
        poolAccountDerivation: PoolAccountDerivation,
        stakingSharedState: StakingSharedState,
        nominationPoolSharedComputation: NominationPoolSharedComputation,
        stakingSharedComputation: StakingSharedComputation,
    ): NominationPoolsRedeemInteractor = RealNominationPoolsRedeemInteractor(
        extrinsicService = extrinsicService,
        stakingRepository = stakingRepository,
        poolAccountDerivation = poolAccountDerivation,
        stakingSharedState = stakingSharedState,
        nominationPoolSharedComputation = nominationPoolSharedComputation,
        stakingSharedComputation = stakingSharedComputation
    )

    @Provides
    @ScreenScope
    fun provideValidationSystem(): NominationPoolsRedeemValidationSystem {
        return ValidationSystem.nominationPoolsRedeem()
    }

    @Provides
    @IntoMap
    @ViewModelKey(NominationPoolsRedeemViewModel::class)
    fun provideViewModel(
        router: NominationPoolsRouter,
        interactor: NominationPoolsRedeemInteractor,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        validationSystem: NominationPoolsRedeemValidationSystem,
        walletUiUseCase: WalletUiUseCase,
        selectedAccountUseCase: SelectedAccountUseCase,
        stakingSharedState: StakingSharedState,
        externalActions: ExternalActions.Presentation,
        poolMemberUseCase: NominationPoolMemberUseCase,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        assetUseCase: AssetUseCase,
    ): ViewModel {
        return NominationPoolsRedeemViewModel(
            router = router,
            interactor = interactor,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            validationSystem = validationSystem,
            walletUiUseCase = walletUiUseCase,
            selectedAccountUseCase = selectedAccountUseCase,
            stakingSharedState = stakingSharedState,
            externalActions = externalActions,
            poolMemberUseCase = poolMemberUseCase,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            assetUseCase = assetUseCase
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): NominationPoolsRedeemViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(NominationPoolsRedeemViewModel::class.java)
    }
}
