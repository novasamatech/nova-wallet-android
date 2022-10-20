package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.SetupVoteReferendumPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.SetupVoteReferendumViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class SetupVoteReferendumModule {

    @Provides
    @IntoMap
    @ViewModelKey(SetupVoteReferendumViewModel::class)
    fun provideViewModel(
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        assetUseCase: AssetUseCase,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
        interactor: VoteReferendumInteractor,
        payload: SetupVoteReferendumPayload,
        resourceManager: ResourceManager,
        router: GovernanceRouter
    ): ViewModel {
        return SetupVoteReferendumViewModel(
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            assetUseCase = assetUseCase,
            amountChooserMixinFactory = amountChooserMixinFactory,
            interactor = interactor,
            payload = payload,
            resourceManager = resourceManager,
            router = router
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): SetupVoteReferendumViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SetupVoteReferendumViewModel::class.java)
    }
}
