package io.novafoundation.nova.app.root.di.busHandler

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.app.root.presentation.requestBusHandler.CompoundRequestBusHandler
import io.novafoundation.nova.app.root.presentation.requestBusHandler.ProxyExtrinsicValidationRequestBusHandler
import io.novafoundation.nova.app.root.presentation.requestBusHandler.RequestBusHandler
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxyExtrinsicValidationRequestBus
import io.novafoundation.nova.feature_wallet_api.domain.validation.ProxyHaveEnoughFeeValidationFactory

@Module
class RequestBusHandlerModule {

    @Provides
    @FeatureScope
    @IntoSet
    fun provideProxyExtrinsicValidationRequestBusHandler(
        scope: RootScope,
        proxyProxyExtrinsicValidationRequestBus: ProxyExtrinsicValidationRequestBus,
        proxyHaveEnoughFeeValidationFactory: ProxyHaveEnoughFeeValidationFactory,
        resourceManager: ResourceManager
    ): RequestBusHandler {
        return ProxyExtrinsicValidationRequestBusHandler(
            scope,
            proxyProxyExtrinsicValidationRequestBus,
            proxyHaveEnoughFeeValidationFactory,
            resourceManager
        )
    }

    @Provides
    @FeatureScope
    fun provideCompoundRequestBusHandler(
        handlers: Set<@JvmSuppressWildcards RequestBusHandler>
    ): CompoundRequestBusHandler {
        return CompoundRequestBusHandler(handlers)
    }
}
