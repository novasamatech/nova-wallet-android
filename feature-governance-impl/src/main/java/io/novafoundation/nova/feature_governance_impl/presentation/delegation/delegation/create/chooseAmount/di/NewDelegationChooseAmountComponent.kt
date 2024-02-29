package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseAmount.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseAmount.NewDelegationChooseAmountFragment
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.create.chooseAmount.NewDelegationChooseAmountPayload

@Subcomponent(
    modules = [
        NewDelegationChooseAmountModule::class
    ]
)
@ScreenScope
interface NewDelegationChooseAmountComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: NewDelegationChooseAmountPayload,
        ): NewDelegationChooseAmountComponent
    }

    fun inject(fragment: NewDelegationChooseAmountFragment)
}
