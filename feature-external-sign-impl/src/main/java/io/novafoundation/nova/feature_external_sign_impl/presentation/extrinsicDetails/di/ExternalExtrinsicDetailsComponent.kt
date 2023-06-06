package io.novafoundation.nova.feature_external_sign_impl.presentation.extrinsicDetails.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_external_sign_impl.presentation.extrinsicDetails.ExternalExtrinsicDetailsFragment

@Subcomponent(
    modules = [
        ExternalExtrinsicDetailsModule::class
    ]
)
@ScreenScope
interface ExternalExtrinsicDetailsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance extrinsicContent: String,
        ): ExternalExtrinsicDetailsComponent
    }

    fun inject(fragment: ExternalExtrinsicDetailsFragment)
}
