package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main.DelegateDetailsFragment
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegate.detail.main.DelegateDetailsPayload

@Subcomponent(
    modules = [
        DelegateDetailsModule::class
    ]
)
@ScreenScope
interface DelegateDetailsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: DelegateDetailsPayload
        ): DelegateDetailsComponent
    }

    fun inject(fragment: DelegateDetailsFragment)
}
