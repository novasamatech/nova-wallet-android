package io.novafoundation.nova.feature_account_impl.presentation.node.list.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.node.list.NodesFragment

@Subcomponent(
    modules = [
        NodesModule::class
    ]
)
@ScreenScope
interface NodesComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): NodesComponent
    }

    fun inject(fragment: NodesFragment)
}
