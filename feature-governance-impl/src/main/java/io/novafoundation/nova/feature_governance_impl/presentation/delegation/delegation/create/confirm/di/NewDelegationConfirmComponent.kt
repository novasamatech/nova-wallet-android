package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.confirm.NewDelegationConfirmFragment
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.create.confirm.NewDelegationConfirmPayload

@Subcomponent(
    modules = [
        NewDelegationConfirmModule::class
    ]
)
@ScreenScope
interface NewDelegationConfirmComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: NewDelegationConfirmPayload,
        ): NewDelegationConfirmComponent
    }

    fun inject(fragment: NewDelegationConfirmFragment)
}
