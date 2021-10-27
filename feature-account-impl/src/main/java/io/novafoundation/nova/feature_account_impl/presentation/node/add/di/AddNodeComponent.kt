package io.novafoundation.nova.feature_account_impl.presentation.node.add.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.node.add.AddNodeFragment

@Subcomponent(
    modules = [
        AddNodeModule::class
    ]
)
@ScreenScope
interface AddNodeComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): AddNodeComponent
    }

    fun inject(fragment: AddNodeFragment)
}
