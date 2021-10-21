package io.novafoundation.nova.feature_staking_impl.presentation.payouts.detail.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.detail.PayoutDetailsFragment
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.model.PendingPayoutParcelable

@Subcomponent(
    modules = [
        PayoutDetailsModule::class
    ]
)
@ScreenScope
interface PayoutDetailsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payout: PendingPayoutParcelable
        ): PayoutDetailsComponent
    }

    fun inject(fragment: PayoutDetailsFragment)
}
