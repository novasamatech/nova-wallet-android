package io.novafoundation.nova.feature_external_sign_impl.presentation.signExtrinsic.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignPayload
import io.novafoundation.nova.feature_external_sign_impl.presentation.signExtrinsic.ExternalSignFragment

@Subcomponent(
    modules = [
        ExternalSignModule::class
    ]
)
@ScreenScope
interface ExternalSignComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ExternalSignPayload,
        ): ExternalSignComponent
    }

    fun inject(fragment: ExternalSignFragment)
}
