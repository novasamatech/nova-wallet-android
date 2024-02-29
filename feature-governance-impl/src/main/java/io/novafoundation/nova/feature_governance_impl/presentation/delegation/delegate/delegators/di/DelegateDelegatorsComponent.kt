package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.delegators.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.delegators.DelegateDelegatorsFragment
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegate.delegators.DelegateDelegatorsPayload

@Subcomponent(
    modules = [
        DelegateDelegatorsModule::class
    ]
)
@ScreenScope
interface DelegateDelegatorsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance parcelable: DelegateDelegatorsPayload,
        ): DelegateDelegatorsComponent
    }

    fun inject(fragment: DelegateDelegatorsFragment)
}
