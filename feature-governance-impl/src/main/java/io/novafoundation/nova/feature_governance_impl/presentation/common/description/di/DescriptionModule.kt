package io.novafoundation.nova.feature_governance_impl.presentation.common.description.di

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
import io.novafoundation.nova.feature_governance_impl.presentation.common.description.DescriptionPayload
import io.novafoundation.nova.feature_governance_impl.presentation.common.description.DescriptionViewModel

@Module(includes = [ViewModelModule::class, MarkdownFullModule::class])
class DescriptionModule {

    @Provides
    @IntoMap
    @ViewModelKey(DescriptionViewModel::class)
    fun provideViewModel(
        router: GovernanceRouter,
        payload: DescriptionPayload,
        markwon: Markwon
    ): ViewModel {
        return DescriptionViewModel(
            router = router,
            payload = payload,
            markwon = markwon
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): DescriptionViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(DescriptionViewModel::class.java)
    }
}
