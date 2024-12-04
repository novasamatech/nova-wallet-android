package io.novafoundation.nova.feature_dapp_impl.presentation.favorites.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_dapp_impl.presentation.favorites.DAppFavoritesViewModel
import io.novafoundation.nova.feature_dapp_impl.presentation.favorites.DappFavoritesFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DappSearchFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.search.SearchPayload

@Subcomponent(
    modules = [
        DAppFavoritesViewModel::class
    ]
)
@ScreenScope
interface DAppFavoritesComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): DAppFavoritesComponent
    }

    fun inject(fragment: DappFavoritesFragment)
}
