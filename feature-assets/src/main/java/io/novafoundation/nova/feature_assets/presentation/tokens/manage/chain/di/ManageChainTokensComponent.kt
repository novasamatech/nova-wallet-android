package io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain.ManageChainTokensFragment
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain.ManageChainTokensPayload

@Subcomponent(
    modules = [
        ManageChainTokensModule::class
    ]
)
@ScreenScope
interface ManageChainTokensComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ManageChainTokensPayload,
        ): ManageChainTokensComponent
    }

    fun inject(fragment: ManageChainTokensFragment)
}
