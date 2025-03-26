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
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.referendum.VoteReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.conviction.ConvictionValuesProvider
import io.novafoundation.nova.feature_governance_impl.presentation.common.locks.LocksFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.SetupVotePayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.referenda.SetupReferendumVoteViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory

@Module(includes = [ViewModelModule::class])
class SetupReferendumVoteModule {

    @Provides
    @IntoMap
    @ViewModelKey(SetupReferendumVoteViewModel::class)
    fun provideViewModel(
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
        assetUseCase: AssetUseCase,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
        interactor: VoteReferendumInteractor,
        payload: SetupVotePayload,
        resourceManager: ResourceManager,
        router: GovernanceRouter,
        validationSystem: VoteReferendumValidationSystem,
        validationExecutor: ValidationExecutor,
        referendumFormatter: ReferendumFormatter,
        locksChangeFormatter: LocksChangeFormatter,
        convictionValuesProvider: ConvictionValuesProvider,
        maxActionProviderFactory: MaxActionProviderFactory,
        locksFormatter: LocksFormatter,
    ): ViewModel {
        return SetupReferendumVoteViewModel(
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            assetUseCase = assetUseCase,
            amountChooserMixinFactory = amountChooserMixinFactory,
            interactor = interactor,
            payload = payload,
            resourceManager = resourceManager,
            router = router,
            validationSystem = validationSystem,
            validationExecutor = validationExecutor,
            referendumFormatter = referendumFormatter,
            locksChangeFormatter = locksChangeFormatter,
            convictionValuesProvider = convictionValuesProvider,
            maxActionProviderFactory = maxActionProviderFactory,
            locksFormatter = locksFormatter,
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): SetupReferendumVoteViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SetupReferendumVoteViewModel::class.java)
    }
}
