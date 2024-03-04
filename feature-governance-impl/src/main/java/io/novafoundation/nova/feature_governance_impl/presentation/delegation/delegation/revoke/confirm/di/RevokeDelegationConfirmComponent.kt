package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.confirm.RevokeDelegationConfirmFragment
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.confirm.RevokeDelegationConfirmPayload

@Subcomponent(
    modules = [
        RevokeDelegationConfirmModule::class
    ]
)
@ScreenScope
interface RevokeDelegationConfirmComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: RevokeDelegationConfirmPayload,
        ): RevokeDelegationConfirmComponent
    }

    fun inject(fragment: RevokeDelegationConfirmFragment)
}
