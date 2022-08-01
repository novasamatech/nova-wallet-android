package io.novafoundation.nova.feature_account_impl.presentation.account.create.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.account.create.CreateAccountFragment

@Subcomponent(
    modules = [
        CreateAccountModule::class
    ]
)
@ScreenScope
interface CreateAccountComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): CreateAccountComponent
    }

    fun inject(createAccountFragment: CreateAccountFragment)
}
