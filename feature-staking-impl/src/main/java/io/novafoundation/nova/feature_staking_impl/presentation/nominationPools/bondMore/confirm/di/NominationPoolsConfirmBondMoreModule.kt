package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.NominationPoolsBondMoreInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.NominationPoolsBondMoreValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.common.di.NominationPoolsCommonBondMoreModule
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm.NominationPoolsConfirmBondMorePayload
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm.NominationPoolsConfirmBondMoreViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.hints.NominationPoolsBondMoreHintsFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase

@Module(includes = [ViewModelModule::class, NominationPoolsCommonBondMoreModule::class])
class NominationPoolsConfirmBondMoreModule {

    @Provides
    @IntoMap
    @ViewModelKey(NominationPoolsConfirmBondMoreViewModel::class)
    fun provideViewModel(
        router: NominationPoolsRouter,
        interactor: NominationPoolsBondMoreInteractor,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        validationSystem: NominationPoolsBondMoreValidationSystem,
        externalActions: ExternalActions.Presentation,
        stakingSharedState: StakingSharedState,
        payload: NominationPoolsConfirmBondMorePayload,
        poolMemberUseCase: NominationPoolMemberUseCase,
        hintsFactory: NominationPoolsBondMoreHintsFactory,
        assetUseCase: AssetUseCase,
        walletUiUseCase: WalletUiUseCase,
        selectedAccountUseCase: SelectedAccountUseCase,
    ): ViewModel {
        return NominationPoolsConfirmBondMoreViewModel(
            router = router,
            interactor = interactor,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            validationSystem = validationSystem,
            externalActions = externalActions,
            stakingSharedState = stakingSharedState,
            payload = payload,
            poolMemberUseCase = poolMemberUseCase,
            hintsFactory = hintsFactory,
            assetUseCase = assetUseCase,
            walletUiUseCase = walletUiUseCase,
            selectedAccountUseCase = selectedAccountUseCase
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): NominationPoolsConfirmBondMoreViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(NominationPoolsConfirmBondMoreViewModel::class.java)
    }
}
