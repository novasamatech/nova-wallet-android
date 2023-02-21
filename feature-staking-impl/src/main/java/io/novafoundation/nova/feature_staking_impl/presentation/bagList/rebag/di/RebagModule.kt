package io.novafoundation.nova.feature_staking_impl.presentation.bagList.rebag.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.repository.BagListRepository
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bagList.rebag.RealRebagInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bagList.rebag.RebagInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bagList.rebag.validations.RebagValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bagList.rebag.validations.rebagValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bagList.rebag.RebagViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository

@Module(includes = [ViewModelModule::class])
class RebagModule {

    @Provides
    @ScreenScope
    fun provideValidationSystem() = ValidationSystem.rebagValidationSystem()

    @Provides
    @ScreenScope
    fun provideInteractor(
        totalIssuanceRepository: TotalIssuanceRepository,
        bagListRepository: BagListRepository,
        assetUseCase: AssetUseCase,
        extrinsicService: ExtrinsicService,
    ): RebagInteractor {
        return RealRebagInteractor(
            totalIssuanceRepository = totalIssuanceRepository,
            bagListRepository = bagListRepository,
            assetUseCase = assetUseCase,
            extrinsicService = extrinsicService
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(RebagViewModel::class)
    fun provideViewModel(
        interactor: RebagInteractor,
        stakingInteractor: StakingInteractor,
        stakingSharedState: StakingSharedState,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        walletUiUseCase: WalletUiUseCase,
        router: StakingRouter,
        externalActions: ExternalActions.Presentation,
        validationExecutor: ValidationExecutor,
        validationSystem: RebagValidationSystem,
        resourceManager: ResourceManager,
        iconGenerator: AddressIconGenerator,
        resourcesHintsMixinFactory: ResourcesHintsMixinFactory,
    ): ViewModel {
        return RebagViewModel(
            interactor = interactor,
            stakingInteractor = stakingInteractor,
            stakingSharedState = stakingSharedState,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            walletUiUseCase = walletUiUseCase,
            router = router,
            validationSystem = validationSystem,
            externalActions = externalActions,
            validationExecutor = validationExecutor,
            resourceManager = resourceManager,
            iconGenerator = iconGenerator,
            resourcesHintsMixinFactory = resourcesHintsMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): RebagViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(RebagViewModel::class.java)
    }
}
