package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.referenda.di

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
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_api.domain.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.VoteReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.conviction.ConvictionValuesProvider
import io.novafoundation.nova.feature_governance_impl.presentation.common.locks.LocksFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.SetupVotePayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.SetupTinderGovVoteViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.TinderGovVoteCommunicator
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class SetupTinderGovVoteModule {

    @Provides
    @IntoMap
    @ViewModelKey(SetupTinderGovVoteViewModel::class)
    fun provideViewModel(
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        assetUseCase: AssetUseCase,
        tinderGovInteractor: TinderGovInteractor,
        tinderGovVoteCommunicator: TinderGovVoteCommunicator,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
        interactor: VoteReferendumInteractor,
        payload: SetupVotePayload,
        resourceManager: ResourceManager,
        router: GovernanceRouter,
        validationSystem: VoteReferendumValidationSystem,
        validationExecutor: ValidationExecutor,
        locksChangeFormatter: LocksChangeFormatter,
        convictionValuesProvider: ConvictionValuesProvider,
        locksFormatter: LocksFormatter
    ): ViewModel {
        return SetupTinderGovVoteViewModel(
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            assetUseCase = assetUseCase,
            amountChooserMixinFactory = amountChooserMixinFactory,
            interactor = interactor,
            payload = payload,
            resourceManager = resourceManager,
            router = router,
            validationSystem = validationSystem,
            validationExecutor = validationExecutor,
            locksChangeFormatter = locksChangeFormatter,
            convictionValuesProvider = convictionValuesProvider,
            locksFormatter = locksFormatter,
            tinderGovInteractor = tinderGovInteractor,
            tinderGovVoteResponder = tinderGovVoteCommunicator
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): SetupTinderGovVoteViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SetupTinderGovVoteViewModel::class.java)
    }
}
