package io.novafoundation.nova.feature_account_impl.di.modules.signers

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationService
import io.novafoundation.nova.common.utils.DefaultMutableSharedState
import io.novafoundation.nova.common.utils.MutableSharedState
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.repository.ProxyRepository
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_api.presenatation.account.watchOnly.WatchOnlyMissingKeysPresenter
import io.novafoundation.nova.feature_account_api.presenatation.account.proxy.ProxySigningPresenter
import io.novafoundation.nova.feature_account_api.presenatation.sign.LedgerSignCommunicator
import io.novafoundation.nova.feature_account_impl.data.signer.RealSignerProvider
import io.novafoundation.nova.feature_account_impl.data.signer.ledger.LedgerSigner
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.ParitySignerSigner
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.PolkadotVaultSigner
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.PolkadotVaultVariantSignCommunicator
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.ProxiedFeeSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.ProxiedSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.validation.ProxiedExtrinsicValidationSystem
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.validation.ProxyHaveEnoughFeeValidationFactory
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.validation.proxiedExtrinsicValidationSystem
import io.novafoundation.nova.feature_account_impl.data.signer.secrets.SecretsSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.watchOnly.WatchOnlySigner
import io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported.SigningNotSupportedPresentable
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic

@Module
class ProxiedSignerModule {

    @Provides
    @FeatureScope
    fun provideProxyHaveEnoughFeeValidationFactory(
        assetSourceRegistry: AssetSourceRegistry,
        accountRepository: AccountRepository,
        walletRepository: WalletRepository,
        extrinsicService: ExtrinsicService
    ) = ProxyHaveEnoughFeeValidationFactory(
        assetSourceRegistry,
        accountRepository,
        walletRepository,
        extrinsicService
    )

    @Provides
    @FeatureScope
    fun provideProxiedExtrinsicValidationSystem(
        proxyHaveEnoughFeeValidationFactory: ProxyHaveEnoughFeeValidationFactory
    ): ProxiedExtrinsicValidationSystem {
        return proxiedExtrinsicValidationSystem(proxyHaveEnoughFeeValidationFactory)
    }

    @Provides
    @FeatureScope
    fun provideProxiedSignerFactory(
        secretStoreV2: SecretStoreV2,
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
        proxySigningPresenter: ProxySigningPresenter,
        proxyRepository: ProxyRepository,
        proxiedExtrinsicValidationSystem: ProxiedExtrinsicValidationSystem,
    ) = ProxiedSignerFactory(secretStoreV2, chainRegistry, accountRepository, proxySigningPresenter, proxyRepository, proxiedExtrinsicValidationSystem)

}
