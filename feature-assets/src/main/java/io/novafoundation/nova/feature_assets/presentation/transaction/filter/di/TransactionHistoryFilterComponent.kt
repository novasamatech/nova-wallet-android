package io.novafoundation.nova.feature_assets.presentation.transaction.filter.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.transaction.filter.TransactionHistoryFilterFragment
import io.novafoundation.nova.feature_assets.presentation.transaction.filter.TransactionHistoryFilterPayload

@Subcomponent(
    modules = [
        TransactionHistoryFilterModule::class
    ]
)
@ScreenScope
interface TransactionHistoryFilterComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: TransactionHistoryFilterPayload,
        ): TransactionHistoryFilterComponent
    }

    fun inject(fragment: TransactionHistoryFilterFragment)
}
