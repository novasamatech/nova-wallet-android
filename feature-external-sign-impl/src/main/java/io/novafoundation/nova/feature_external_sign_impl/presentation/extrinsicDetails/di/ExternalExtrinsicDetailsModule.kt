package io.novafoundation.nova.feature_external_sign_impl.presentation.extrinsicDetails.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_external_sign_impl.ExternalSignRouter
import io.novafoundation.nova.feature_external_sign_impl.presentation.extrinsicDetails.ExternalExtrinsicDetailsViewModel

@Module(includes = [ViewModelModule::class])
class ExternalExtrinsicDetailsModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): ExternalExtrinsicDetailsViewModel {
        return ViewModelProvider(fragment, factory).get(ExternalExtrinsicDetailsViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(ExternalExtrinsicDetailsViewModel::class)
    fun provideViewModel(
        router: ExternalSignRouter,
        extrinsicContent: String
    ): ViewModel {
        return ExternalExtrinsicDetailsViewModel(
            router = router,
            extrinsicContent = extrinsicContent
        )
    }
}
