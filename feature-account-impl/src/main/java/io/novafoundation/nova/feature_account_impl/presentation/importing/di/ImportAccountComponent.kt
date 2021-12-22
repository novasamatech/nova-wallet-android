package io.novafoundation.nova.feature_account_impl.presentation.importing.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.importing.ImportAccountFragment

@Subcomponent(
    modules = [
        ImportAccountModule::class
    ]
)
@ScreenScope
interface ImportAccountComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ImportAccountPayload
        ): ImportAccountComponent
    }

    fun inject(importAccountFragment: ImportAccountFragment)
}
