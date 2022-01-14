package io.novafoundation.nova.feature_dapp_impl.presentation.search.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DappSearchFragment

@Subcomponent(
    modules = [
        DAppSearchModule::class
    ]
)
@ScreenScope
interface DAppSearchComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): DAppSearchComponent
    }

    fun inject(fragment: DappSearchFragment)
}
