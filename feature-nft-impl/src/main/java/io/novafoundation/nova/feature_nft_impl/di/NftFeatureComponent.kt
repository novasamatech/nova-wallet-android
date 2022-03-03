package io.novafoundation.nova.feature_nft_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_nft_api.NftFeatureApi
import io.novafoundation.nova.feature_nft_impl.NftRouter
import io.novafoundation.nova.feature_nft_impl.presentation.nft.details.di.NftDetailsComponent
import io.novafoundation.nova.feature_nft_impl.presentation.nft.list.di.NftListComponent
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        NftFeatureDependencies::class
    ],
    modules = [
        NftFeatureModule::class
    ]
)
@FeatureScope
interface NftFeatureComponent : NftFeatureApi {

    fun nftListComponentFactory(): NftListComponent.Factory

    fun nftDetailsComponentFactory(): NftDetailsComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: NftRouter,
            deps: NftFeatureDependencies
        ): NftFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            DbApi::class,
            AccountFeatureApi::class,
            WalletFeatureApi::class,
            RuntimeApi::class
        ]
    )
    interface NftFeatureDependenciesComponent : NftFeatureDependencies
}
