package io.novafoundation.nova.feature_assets.presentation.tokens.manage.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.ManageTokensFragment

@Subcomponent(
    modules = [
        ManageTokensModule::class
    ]
)
@ScreenScope
interface ManageTokensComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): ManageTokensComponent
    }

    fun inject(fragment: ManageTokensFragment)
}
