package io.novafoundation.nova.feature_multisig_operations.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_deep_linking.di.DeepLinkingFeatureApi
import io.novafoundation.nova.feature_multisig_operations.di.deeplink.DeepLinkModule
import io.novafoundation.nova.feature_multisig_operations.presentation.created.di.MultisigCreatedComponent
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter
import io.novafoundation.nova.feature_multisig_operations.presentation.details.full.di.MultisigOperationFullDetailsComponent
import io.novafoundation.nova.feature_multisig_operations.presentation.details.general.di.MultisigOperationDetailsComponent
import io.novafoundation.nova.feature_multisig_operations.presentation.enterCall.di.MultisigOperationEnterCallComponent
import io.novafoundation.nova.feature_multisig_operations.presentation.list.di.MultisigPendingOperationsComponent
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        MultisigOperationsFeatureDependencies::class,
    ],
    modules = [
        MultisigOperationsFeatureModule::class,
        DeepLinkModule::class
    ]
)
@FeatureScope
interface MultisigOperationsFeatureComponent : MultisigOperationsFeatureApi {

    fun multisigPendingOperations(): MultisigPendingOperationsComponent.Factory

    fun multisigOperationDetails(): MultisigOperationDetailsComponent.Factory

    fun multisigOperationFullDetails(): MultisigOperationFullDetailsComponent.Factory

    fun multisigOperationEnterCall(): MultisigOperationEnterCallComponent.Factory

    fun multisigCreated(): MultisigCreatedComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: MultisigOperationsRouter,
            deps: MultisigOperationsFeatureDependencies
        ): MultisigOperationsFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            RuntimeApi::class,
            DbApi::class,
            WalletFeatureApi::class,
            AccountFeatureApi::class,
            DeepLinkingFeatureApi::class
        ]
    )
    interface MultisigOperationsFeatureDependenciesComponent : MultisigOperationsFeatureDependencies
}
