package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.confirm.ConfirmContributeFragment
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.confirm.parcel.ConfirmContributePayload

@Subcomponent(
    modules = [
        ConfirmContributeModule::class
    ]
)
@ScreenScope
interface ConfirmContributeComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ConfirmContributePayload
        ): ConfirmContributeComponent
    }

    fun inject(fragment: ConfirmContributeFragment)
}
