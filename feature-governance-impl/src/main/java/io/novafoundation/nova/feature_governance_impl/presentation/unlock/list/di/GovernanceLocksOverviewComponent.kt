package io.novafoundation.nova.feature_governance_impl.presentation.unlock.list.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.unlock.list.GovernanceLocksOverviewFragment

@Subcomponent(
    modules = [
        GovernanceLocksOverviewModule::class
    ]
)
@ScreenScope
interface GovernanceLocksOverviewComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): GovernanceLocksOverviewComponent
    }

    fun inject(fragment: GovernanceLocksOverviewFragment)
}
