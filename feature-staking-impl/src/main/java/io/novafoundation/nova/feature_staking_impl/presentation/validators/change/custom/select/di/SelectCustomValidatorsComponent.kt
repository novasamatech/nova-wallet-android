package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.select.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.common.CustomValidatorsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.select.SelectCustomValidatorsFragment

@Subcomponent(
    modules = [
        SelectCustomValidatorsModule::class
    ]
)
@ScreenScope
interface SelectCustomValidatorsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance argument: CustomValidatorsPayload
        ): SelectCustomValidatorsComponent
    }

    fun inject(fragment: SelectCustomValidatorsFragment)
}
