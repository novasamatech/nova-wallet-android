package io.novafoundation.nova.feature_nft_impl.presentation.nft.list.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_nft_impl.presentation.nft.list.NftListFragment

@Subcomponent(
    modules = [
        NftListModule::class
    ]
)
@ScreenScope
interface NftListComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): NftListComponent
    }

    fun inject(fragment: NftListFragment)
}
