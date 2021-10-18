package jp.co.soramitsu.feature_staking_impl.presentation.payouts.confirm.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import jp.co.soramitsu.common.address.AddressIconGenerator
import jp.co.soramitsu.common.di.viewmodel.ViewModelKey
import jp.co.soramitsu.common.di.viewmodel.ViewModelModule
import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.common.validation.ValidationExecutor
import jp.co.soramitsu.common.validation.ValidationSystem
import jp.co.soramitsu.feature_account_api.presenatation.account.AddressDisplayUseCase
import jp.co.soramitsu.feature_account_api.presenatation.actions.ExternalActions
import jp.co.soramitsu.feature_staking_impl.data.StakingSharedState
import jp.co.soramitsu.feature_staking_impl.domain.StakingInteractor
import jp.co.soramitsu.feature_staking_impl.domain.payout.PayoutInteractor
import jp.co.soramitsu.feature_staking_impl.domain.validations.payout.MakePayoutPayload
import jp.co.soramitsu.feature_staking_impl.domain.validations.payout.PayoutValidationFailure
import jp.co.soramitsu.feature_staking_impl.presentation.StakingRouter
import jp.co.soramitsu.feature_staking_impl.presentation.payouts.confirm.ConfirmPayoutViewModel
import jp.co.soramitsu.feature_staking_impl.presentation.payouts.confirm.model.ConfirmPayoutPayload
import jp.co.soramitsu.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class ConfirmPayoutModule {

    @Provides
    @IntoMap
    @ViewModelKey(ConfirmPayoutViewModel::class)
    fun provideViewModel(
        interactor: StakingInteractor,
        router: StakingRouter,
        payload: ConfirmPayoutPayload,
        payoutInteractor: PayoutInteractor,
        addressIconGenerator: AddressIconGenerator,
        externalActions: ExternalActions.Presentation,
        feeLoaderMixin: FeeLoaderMixin.Presentation,
        validationSystem: ValidationSystem<MakePayoutPayload, PayoutValidationFailure>,
        validationExecutor: ValidationExecutor,
        addressDisplayUseCase: AddressDisplayUseCase,
        resourceManager: ResourceManager,
        singleAssetSharedState: StakingSharedState,
    ): ViewModel {
        return ConfirmPayoutViewModel(
            interactor,
            payoutInteractor,
            router,
            payload,
            addressIconGenerator,
            externalActions,
            feeLoaderMixin,
            addressDisplayUseCase,
            validationSystem,
            validationExecutor,
            resourceManager,
            singleAssetSharedState
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ConfirmPayoutViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmPayoutViewModel::class.java)
    }
}
