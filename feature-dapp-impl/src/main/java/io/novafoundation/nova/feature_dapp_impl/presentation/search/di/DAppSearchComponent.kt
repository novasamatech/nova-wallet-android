package io.novafoundation.nova.feature_dapp_impl.presentation.search.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DappSearchFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.search.SearchPayload

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
            @BindsInstance payload: SearchPayload,
        ): DAppSearchComponent
    }

    fun inject(fragment: DappSearchFragment)
}
