package io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites.AddToFavouritesFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites.AddToFavouritesPayload

@Subcomponent(
    modules = [
        AddToFavouritesModule::class
    ]
)
@ScreenScope
interface AddToFavouritesComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: AddToFavouritesPayload,
        ): AddToFavouritesComponent
    }

    fun inject(fragment: AddToFavouritesFragment)
}
