package io.novafoundation.nova.feature_governance_impl.presentation.common.description.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.common.info.ReferendumInfoFragment
import io.novafoundation.nova.feature_governance_impl.presentation.common.info.ReferendumInfoPayload
import io.novafoundation.nova.feature_governance_impl.presentation.common.info.di.ReferendumInfoModule

@Subcomponent(
    modules = [
        ReferendumInfoModule::class
    ]
)
@ScreenScope
interface ReferendumInfoComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ReferendumInfoPayload,
        ): ReferendumInfoComponent
    }

    fun inject(fragment: ReferendumInfoFragment)
}
