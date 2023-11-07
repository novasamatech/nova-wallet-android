package io.novafoundation.nova.feature_nft_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressCommunicator
import io.novafoundation.nova.feature_assets.presentation.receive.flow.di.NftReceiveFlowComponent
import io.novafoundation.nova.feature_assets.presentation.send.amount.di.InputAddressNftComponent
import io.novafoundation.nova.feature_assets.presentation.send.flow.di.NftSendFlowComponent
import io.novafoundation.nova.feature_nft_api.NftFeatureApi
import io.novafoundation.nova.feature_nft_impl.NftRouter
import io.novafoundation.nova.feature_nft_impl.presentation.nft.details.di.NftDetailsComponent
import io.novafoundation.nova.feature_nft_impl.presentation.nft.list.di.NftListComponent
import io.novafoundation.nova.feature_nft_impl.presentation.nft.send.confirm.di.ConfirmNftSendComponent
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

    fun inputAddressNftComponentFactory(): InputAddressNftComponent.Factory

    fun confirmSendComponentFactory(): ConfirmNftSendComponent.Factory

    fun nftReceiveFlowComponentFactory(): NftReceiveFlowComponent.Factory

    fun nftSendFlowComponentFactory(): NftSendFlowComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: NftRouter,
            @BindsInstance selectAddressCommunicator: SelectAddressCommunicator,
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
