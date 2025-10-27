package io.novafoundation.nova.feature_gift_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_gift_api.di.GiftFeatureApi
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter
import io.novafoundation.nova.feature_gift_impl.presentation.amount.di.SelectGiftAmountComponent
import io.novafoundation.nova.feature_gift_impl.presentation.gifts.di.GiftsComponent
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        GiftFeatureDependencies::class
    ],
    modules = [
        GiftFeatureModule::class
    ]
)
@FeatureScope
interface GiftFeatureComponent : GiftFeatureApi {

    fun giftsComponentFactory(): GiftsComponent.Factory

    fun selectGiftAmountComponentFactory(): SelectGiftAmountComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: GiftRouter,
            deps: GiftFeatureDependencies
        ): GiftFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            AccountFeatureApi::class,
            DbApi::class,
            WalletFeatureApi::class,
            RuntimeApi::class
        ]
    )
    interface GiftFeatureDependenciesComponent : GiftFeatureDependencies
}
