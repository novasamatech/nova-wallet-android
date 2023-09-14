package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.confirm.ConfirmChangeValidatorsFragment

@Subcomponent(
    modules = [
        ConfirmChangeValidatorsModule::class
    ]
)
@ScreenScope
interface ConfirmChangeValidatorsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): ConfirmChangeValidatorsComponent
    }

    fun inject(fragment: ConfirmChangeValidatorsFragment)
}
