package io.novafoundation.nova.feature_governance_impl.presentation.referenda.description.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.noties.markwon.Markwon
import io.novafoundation.nova.common.di.modules.shared.MarkdownFullModule
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.description.ReferendumDescriptionPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.description.ReferendumDescriptionViewModel

@Module(includes = [ViewModelModule::class, MarkdownFullModule::class])
class ReferendumDescriptionModule {

    @Provides
    @IntoMap
    @ViewModelKey(ReferendumDescriptionViewModel::class)
    fun provideViewModel(
        router: GovernanceRouter,
        payload: ReferendumDescriptionPayload,
        markwon: Markwon
    ): ViewModel {
        return ReferendumDescriptionViewModel(
            router = router,
            payload = payload,
            markwon = markwon
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ReferendumDescriptionViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ReferendumDescriptionViewModel::class.java)
    }
}
