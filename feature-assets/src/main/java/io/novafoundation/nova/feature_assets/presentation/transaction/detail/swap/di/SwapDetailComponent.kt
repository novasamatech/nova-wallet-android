package io.novafoundation.nova.feature_assets.presentation.transaction.detail.swap.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.swap.SwapDetailFragment

@Subcomponent(
    modules = [
        SwapDetailModule::class
    ]
)
@ScreenScope
interface SwapDetailComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance swap: OperationParcelizeModel.Swap
        ): SwapDetailComponent
    }

    fun inject(fragment: SwapDetailFragment)
}
