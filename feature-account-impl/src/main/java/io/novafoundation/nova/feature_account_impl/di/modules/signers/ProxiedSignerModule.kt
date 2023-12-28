package io.novafoundation.nova.feature_account_impl.di.modules.signers

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.repository.ProxyRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.proxy.ProxySigningPresenter
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.ProxiedSignerFactory
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxyExtrinsicValidationRequestBus
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.rpc.RpcCalls

@Module
class ProxiedSignerModule {

    @Provides
    @FeatureScope
    fun provideProxyExtrinsicValidationRequestBus() = ProxyExtrinsicValidationRequestBus()

    @Provides
    @FeatureScope
    fun provideProxiedSignerFactory(
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
        proxySigningPresenter: ProxySigningPresenter,
        proxyRepository: ProxyRepository,
        rpcCalls: RpcCalls,
        proxyExtrinsicValidationRequestBus: ProxyExtrinsicValidationRequestBus,
    ) = ProxiedSignerFactory(
        chainRegistry,
        accountRepository,
        proxySigningPresenter,
        proxyRepository,
        rpcCalls,
        proxyExtrinsicValidationRequestBus
    )
}
