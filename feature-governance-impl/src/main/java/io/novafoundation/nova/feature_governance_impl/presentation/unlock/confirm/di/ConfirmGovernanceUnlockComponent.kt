package io.novafoundation.nova.feature_governance_impl.presentation.unlock.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.unlock.confirm.ConfirmGovernanceUnlockFragment

@Subcomponent(
    modules = [
        ConfirmGovernanceUnlockModule::class
    ]
)
@ScreenScope
interface ConfirmGovernanceUnlockComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): ConfirmGovernanceUnlockComponent
    }

    fun inject(fragment: ConfirmGovernanceUnlockFragment)
}
