package io.novafoundation.nova.feature_external_sign_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_external_sign_api.di.ExternalSignFeatureApi
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_external_sign_impl.ExternalSignRouter
import io.novafoundation.nova.feature_external_sign_impl.presentation.extrinsicDetails.di.ExternalExtrinsicDetailsComponent
import io.novafoundation.nova.feature_external_sign_impl.presentation.signExtrinsic.di.ExternalSignComponent
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        ExternalSignFeatureDependencies::class
    ],
    modules = [
        ExternalSignFeatureModule::class
    ]
)
@FeatureScope
interface ExternalSignFeatureComponent : ExternalSignFeatureApi {

    fun signExtrinsicComponentFactory(): ExternalSignComponent.Factory

    fun extrinsicDetailsComponentFactory(): ExternalExtrinsicDetailsComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: ExternalSignRouter,
            @BindsInstance signCommunicator: ExternalSignCommunicator,
            deps: ExternalSignFeatureDependencies
        ): ExternalSignFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            AccountFeatureApi::class,
            WalletFeatureApi::class,
            RuntimeApi::class,
            CurrencyFeatureApi::class,
        ]
    )
    interface ExternalSignFeatureDependenciesComponent : ExternalSignFeatureDependencies
}
