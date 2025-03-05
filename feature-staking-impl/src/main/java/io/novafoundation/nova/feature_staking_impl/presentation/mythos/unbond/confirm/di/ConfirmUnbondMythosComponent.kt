package io.novafoundation.nova.feature_staking_impl.presentation.mythos.unbond.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.unbond.confirm.ConfirmUnbondMythosFragment
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.unbond.confirm.ConfirmUnbondMythosPayload

@Subcomponent(
    modules = [
        ConfirmUnbondMythosModule::class
    ]
)
@ScreenScope
interface ConfirmUnbondMythosComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ConfirmUnbondMythosPayload
        ): ConfirmUnbondMythosComponent
    }

    fun inject(fragment: ConfirmUnbondMythosFragment)
}
