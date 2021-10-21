package io.novafoundation.nova.feature_account_impl.presentation.node.details.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.node.details.NodeDetailsFragment

@Subcomponent(
    modules = [
        NodeDetailsModule::class
    ]
)
@ScreenScope
interface NodeDetailsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance nodeId: Int,
        ): NodeDetailsComponent
    }

    fun inject(fragment: NodeDetailsFragment)
}
