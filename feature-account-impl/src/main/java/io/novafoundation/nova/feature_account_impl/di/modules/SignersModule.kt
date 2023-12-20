package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationService
import io.novafoundation.nova.common.utils.DefaultMutableSharedState
import io.novafoundation.nova.common.utils.MutableSharedState
import io.novafoundation.nova.feature_account_api.data.repository.ProxyRepository
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_api.presenatation.account.proxy.ProxySigningPresenter
import io.novafoundation.nova.feature_account_api.presenatation.account.watchOnly.WatchOnlyMissingKeysPresenter
import io.novafoundation.nova.feature_account_api.presenatation.sign.LedgerSignCommunicator
import io.novafoundation.nova.feature_account_impl.data.signer.RealSignerProvider
import io.novafoundation.nova.feature_account_impl.data.signer.ledger.LedgerSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.PolkadotVaultVariantSignCommunicator
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.PolkadotVaultVariantSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.ProxiedFeeSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.ProxiedSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.secrets.SecretsSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.watchOnly.WatchOnlySignerFactory
import io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported.SigningNotSupportedPresentable
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic

@Module
class SignersModule {

    @Provides
    @FeatureScope
    fun provideSignSharedState(): MutableSharedState<SignerPayloadExtrinsic> = DefaultMutableSharedState()

    @Provides
    @FeatureScope
    fun provideSecretsSignerFactory(
        secretStoreV2: SecretStoreV2,
        chainRegistry: ChainRegistry,
        twoFactorVerificationService: TwoFactorVerificationService
    ) = SecretsSignerFactory(secretStoreV2, chainRegistry, twoFactorVerificationService)

    @Provides
    @FeatureScope
    fun provideProxiedSignerFactory(
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
        proxySigningPresenter: ProxySigningPresenter,
        proxyRepository: ProxyRepository,
        rpcCalls: RpcCalls
    ) = ProxiedSignerFactory(chainRegistry, accountRepository, proxySigningPresenter, proxyRepository, rpcCalls)

    @Provides
    @FeatureScope
    fun provideProxiedFeeSignerFactory(
        accountRepository: AccountRepository
    ) = ProxiedFeeSignerFactory(accountRepository)

    @Provides
    @FeatureScope
    fun provideWatchOnlySignerFactory(
        watchOnlySigningPresenter: WatchOnlyMissingKeysPresenter
    ) = WatchOnlySignerFactory(watchOnlySigningPresenter)

    @Provides
    @FeatureScope
    fun providePolkadotVaultVariantSignerFactory(
        signingSharedState: MutableSharedState<SignerPayloadExtrinsic>,
        communicator: PolkadotVaultVariantSignCommunicator,
        resourceManager: ResourceManager,
        polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
        signingNotSupportedPresentable: SigningNotSupportedPresentable
    ) = PolkadotVaultVariantSignerFactory(
        signingSharedState = signingSharedState,
        signFlowRequester = communicator,
        resourceManager = resourceManager,
        polkadotVaultVariantConfigProvider = polkadotVaultVariantConfigProvider,
        messageSigningNotSupported = signingNotSupportedPresentable
    )

    @Provides
    @FeatureScope
    fun provideLedgerSignerFactory(
        signingSharedState: MutableSharedState<SignerPayloadExtrinsic>,
        communicator: LedgerSignCommunicator,
        resourceManager: ResourceManager,
        signingNotSupportedPresentable: SigningNotSupportedPresentable
    ) = LedgerSignerFactory(signingSharedState, communicator, resourceManager, signingNotSupportedPresentable)

    @Provides
    @FeatureScope
    fun provideSignerProvider(
        secretsSignerFactory: SecretsSignerFactory,
        proxiedSignerFactory: ProxiedSignerFactory,
        watchOnlySignerFactory: WatchOnlySignerFactory,
        polkadotVaultSignerFactory: PolkadotVaultVariantSignerFactory,
        proxiedFeeSignerFactory: ProxiedFeeSignerFactory,
        ledgerSignerFactory: LedgerSignerFactory
    ): SignerProvider = RealSignerProvider(
        secretsSignerFactory = secretsSignerFactory,
        watchOnlySigner = watchOnlySignerFactory,
        polkadotVaultSignerFactory = polkadotVaultSignerFactory,
        proxiedSignerFactory = proxiedSignerFactory,
        proxiedFeeSignerFactory = proxiedFeeSignerFactory,
        ledgerSignerFactory = ledgerSignerFactory
    )
}
