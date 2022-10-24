package io.novafoundation.nova.feature_governance_impl.presentation.referenda.unlock.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.unlock.ConfirmReferendumUnlockViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class ConfirmReferendumUnlockModule {

    @Provides
    @IntoMap
    @ViewModelKey(ConfirmReferendumUnlockViewModel::class)
    fun provideViewModel(
        governanceRouter: GovernanceRouter,
        feeLoaderMixin: FeeLoaderMixin.Presentation,
        validationExecutor: ValidationExecutor,
        governanceSharedState: GovernanceSharedState,
        externalActions: ExternalActions.Presentation
    ): ViewModel {
        return ConfirmReferendumUnlockViewModel(
            governanceRouter,
            feeLoaderMixin,
            externalActions,
            governanceSharedState,
            validationExecutor
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ConfirmReferendumUnlockViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmReferendumUnlockViewModel::class.java)
    }
}
