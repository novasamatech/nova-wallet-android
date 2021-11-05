package io.novafoundation.nova.feature_account_impl.presentation.account.edit.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.account.edit.AccountEditFragment

@Subcomponent(
    modules = [
        AccountEditModule::class
    ]
)
@ScreenScope
interface AccountEditComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): AccountEditComponent
    }

    fun inject(fragment: AccountEditFragment)
}
