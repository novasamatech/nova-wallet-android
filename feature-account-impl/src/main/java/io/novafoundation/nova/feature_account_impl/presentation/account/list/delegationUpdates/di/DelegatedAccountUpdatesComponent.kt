package io.novafoundation.nova.feature_account_impl.presentation.account.list.delegationUpdates.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.account.list.delegationUpdates.DelegatedAccountUpdatesBottomSheet

@Subcomponent(
    modules = [
        DelegatedAccountUpdatesModule::class
    ]
)
@ScreenScope
interface DelegatedAccountUpdatesComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): DelegatedAccountUpdatesComponent
    }

    fun inject(bottomSheet: DelegatedAccountUpdatesBottomSheet)
}
