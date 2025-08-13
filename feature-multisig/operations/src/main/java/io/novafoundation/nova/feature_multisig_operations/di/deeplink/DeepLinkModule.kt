package io.novafoundation.nova.feature_multisig_operations.di.deeplink

import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkConfigurator
import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.RealMultisigOperationDeepLinkConfigurator
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.DialogMessageManager
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.LinkBuilderFactory
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDetailsDeepLinkHandler
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class DeepLinkModule {

    @Provides
    @FeatureScope
    fun provideDeepLinkConfigurator(
        linkBuilderFactory: LinkBuilderFactory
    ): MultisigOperationDeepLinkConfigurator {
        return RealMultisigOperationDeepLinkConfigurator(linkBuilderFactory)
    }

    @Provides
    @FeatureScope
    fun provideMultisigOperationDetailsDeepLinkHandler(
        router: MultisigOperationsRouter,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        automaticInteractionGate: AutomaticInteractionGate,
        dialogMessageManager: DialogMessageManager,
        multisigCallFormatter: MultisigCallFormatter,
    ): MultisigOperationDetailsDeepLinkHandler {
        return MultisigOperationDetailsDeepLinkHandler(
            router,
            accountRepository,
            chainRegistry,
            automaticInteractionGate,
            dialogMessageManager,
            multisigCallFormatter
        )
    }

    @Provides
    @FeatureScope
    fun provideDeepLinks(operationDeepLink: MultisigOperationDetailsDeepLinkHandler): MultisigDeepLinks {
        return MultisigDeepLinks(listOf(operationDeepLink))
    }
}
