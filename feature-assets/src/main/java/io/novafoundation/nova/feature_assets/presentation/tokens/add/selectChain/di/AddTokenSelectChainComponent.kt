package io.novafoundation.nova.feature_assets.presentation.tokens.add.selectChain.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.tokens.add.selectChain.AddTokenSelectChainFragment

@Subcomponent(
    modules = [
        AddTokenSelectChainModule::class
    ]
)
@ScreenScope
interface AddTokenSelectChainComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): AddTokenSelectChainComponent
    }

    fun inject(fragment: AddTokenSelectChainFragment)
}
