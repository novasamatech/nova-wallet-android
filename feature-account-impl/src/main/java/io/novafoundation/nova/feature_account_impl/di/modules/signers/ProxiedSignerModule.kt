package io.novafoundation.nova.feature_account_impl.di.modules.signers

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.repository.ProxyRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.proxy.ProxySigningPresenter
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.ProxiedSignerFactory
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxyExtrinsicValidationRequestBus
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class ProxiedSignerModule {

    @Provides
    @FeatureScope
    fun provideProxyExtrinsicValidationRequestBus() = ProxyExtrinsicValidationRequestBus()

    @Provides
    @FeatureScope
    fun provideProxiedSignerFactory(
        secretStoreV2: SecretStoreV2,
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
        proxySigningPresenter: ProxySigningPresenter,
        proxyRepository: ProxyRepository,
        proxyExtrinsicValidationRequestBus: ProxyExtrinsicValidationRequestBus
    ) = ProxiedSignerFactory(
        secretStoreV2,
        chainRegistry,
        accountRepository,
        proxySigningPresenter,
        proxyRepository,
        proxyExtrinsicValidationRequestBus
    )
}
