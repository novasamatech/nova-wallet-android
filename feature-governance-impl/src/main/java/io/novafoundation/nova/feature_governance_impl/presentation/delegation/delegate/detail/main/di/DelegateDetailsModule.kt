package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.noties.markwon.Markwon
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.modules.shared.MarkdownShortModule
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.identity.IdentityMixin
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.details.model.DelegateDetailsInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.DelegateMappers
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main.DelegateDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main.DelegateDetailsViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackFormatter

@Module(includes = [ViewModelModule::class, MarkdownShortModule::class])
class DelegateDetailsModule {

    @Provides
    @IntoMap
    @ViewModelKey(DelegateDetailsViewModel::class)
    fun provideViewModel(
        interactor: DelegateDetailsInteractor,
        payload: DelegateDetailsPayload,
        iconGenerator: AddressIconGenerator,
        externalActions: ExternalActions.Presentation,
        identityMixinFactory: IdentityMixin.Factory,
        router: GovernanceRouter,
        delegateMappers: DelegateMappers,
        governanceSharedState: GovernanceSharedState,
        markwon: Markwon,
        trackFormatter: TrackFormatter,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        selectedAccountUseCase: SelectedAccountUseCase
    ): ViewModel {
        return DelegateDetailsViewModel(
            interactor = interactor,
            payload = payload,
            iconGenerator = iconGenerator,
            externalActions = externalActions,
            identityMixinFactory = identityMixinFactory,
            router = router,
            delegateMappers = delegateMappers,
            governanceSharedState = governanceSharedState,
            trackFormatter = trackFormatter,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            selectedAccountUseCase = selectedAccountUseCase,
            markwon = markwon,
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): DelegateDetailsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(DelegateDetailsViewModel::class.java)
    }
}
