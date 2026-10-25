package io.novafoundation.nova.feature_swap_impl.presentation.options.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_swap_api.presentation.model.SlippageSettingsPayload
import io.novafoundation.nova.feature_swap_impl.presentation.options.SlippageSettingsBottomSheet

@Subcomponent(
    modules = [
        SlippageSettingsModule::class
    ]
)
@ScreenScope
interface SlippageSettingsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SlippageSettingsPayload
        ): SlippageSettingsComponent
    }

    fun inject(sheet: SlippageSettingsBottomSheet)
}
