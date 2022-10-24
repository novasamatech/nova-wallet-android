package io.novafoundation.nova.feature_governance_impl.presentation.referenda.unlock.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.unlock.ConfirmReferendumUnlockFragment

@Subcomponent(
    modules = [
        ConfirmReferendumUnlockModule::class
    ]
)
@ScreenScope
interface ConfirmReferendumUnlockComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): ConfirmReferendumUnlockComponent
    }

    fun inject(fragment: ConfirmReferendumUnlockFragment)
}
