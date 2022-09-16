package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.YieldBoostConfirmFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.model.YieldBoostConfirmPayload

@Subcomponent(
    modules = [
        YieldBoostConfirmModule::class
    ]
)
@ScreenScope
interface YieldBoostConfirmComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: YieldBoostConfirmPayload,
        ): YieldBoostConfirmComponent
    }

    fun inject(fragment: YieldBoostConfirmFragment)
}
