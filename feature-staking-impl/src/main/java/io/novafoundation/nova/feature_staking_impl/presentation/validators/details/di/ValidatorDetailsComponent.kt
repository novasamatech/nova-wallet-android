package io.novafoundation.nova.feature_staking_impl.presentation.validators.details.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.ValidatorDetailsFragment
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.ValidatorDetailsParcelModel

@Subcomponent(
    modules = [
        ValidatorDetailsModule::class
    ]
)
@ScreenScope
interface ValidatorDetailsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance validator: ValidatorDetailsParcelModel
        ): ValidatorDetailsComponent
    }

    fun inject(fragment: ValidatorDetailsFragment)
}
