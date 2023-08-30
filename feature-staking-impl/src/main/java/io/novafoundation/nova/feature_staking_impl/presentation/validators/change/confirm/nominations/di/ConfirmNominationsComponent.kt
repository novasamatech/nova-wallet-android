package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.confirm.nominations.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.confirm.nominations.ConfirmNominationsFragment

@Subcomponent(
    modules = [
        ConfirmNominationsModule::class
    ]
)
@ScreenScope
interface ConfirmNominationsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): ConfirmNominationsComponent
    }

    fun inject(fragment: ConfirmNominationsFragment)
}
