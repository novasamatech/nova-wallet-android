package io.novafoundation.nova.feature_staking_impl.presentation.payouts.detail.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.detail.PayoutDetailsViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.model.PendingPayoutParcelable
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter

@Module(includes = [ViewModelModule::class])
class PayoutDetailsModule {

    @Provides
    @IntoMap
    @ViewModelKey(PayoutDetailsViewModel::class)
    fun provideViewModel(
        interactor: StakingInteractor,
        router: StakingRouter,
        payout: PendingPayoutParcelable,
        addressIconGenerator: AddressIconGenerator,
        externalActions: ExternalActions.Presentation,
        resourceManager: ResourceManager,
        selectedAssetState: StakingSharedState,
        amountFormatter: AmountFormatter
    ): ViewModel {
        return PayoutDetailsViewModel(
            interactor,
            router,
            payout,
            addressIconGenerator,
            externalActions,
            resourceManager,
            selectedAssetState,
            amountFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): PayoutDetailsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(PayoutDetailsViewModel::class.java)
    }
}
