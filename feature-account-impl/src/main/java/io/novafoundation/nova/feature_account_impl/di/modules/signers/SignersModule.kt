package io.novafoundation.nova.feature_account_impl.di.modules.signers

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationService
import io.novafoundation.nova.common.utils.DefaultMutableSharedState
import io.novafoundation.nova.common.utils.MutableSharedState
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_api.presenatation.account.watchOnly.WatchOnlyMissingKeysPresenter
import io.novafoundation.nova.feature_account_api.presenatation.sign.LedgerSignCommunicator
import io.novafoundation.nova.feature_account_impl.data.signer.RealSignerProvider
import io.novafoundation.nova.feature_account_impl.data.signer.ledger.LedgerSigner
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.ParitySignerSigner
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.PolkadotVaultSigner
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.PolkadotVaultVariantSignCommunicator
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.ProxiedFeeSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.ProxiedSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.secrets.SecretsSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.watchOnly.WatchOnlySigner
import io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported.SigningNotSupportedPresentable
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic

@Module(includes = [ProxiedSignerModule::class])
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
    fun provideProxiedFeeSignerFactory(
        accountRepository: AccountRepository
    ) = ProxiedFeeSignerFactory(accountRepository)

    @Provides
    @FeatureScope
    fun provideWatchOnlySigner(
        watchOnlySigningPresenter: WatchOnlyMissingKeysPresenter
    ) = WatchOnlySigner(watchOnlySigningPresenter)

    @Provides
    @FeatureScope
    fun provideParitySignerSigner(
        signingSharedState: MutableSharedState<SignerPayloadExtrinsic>,
        communicator: PolkadotVaultVariantSignCommunicator,
        resourceManager: ResourceManager,
        polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
        signingNotSupportedPresentable: SigningNotSupportedPresentable
    ) = ParitySignerSigner(
        signingSharedState = signingSharedState,
        signFlowRequester = communicator,
        resourceManager = resourceManager,
        polkadotVaultVariantConfigProvider = polkadotVaultVariantConfigProvider,
        messageSigningNotSupported = signingNotSupportedPresentable
    )

    @Provides
    @FeatureScope
    fun providePolkadotVaultSigner(
        signingSharedState: MutableSharedState<SignerPayloadExtrinsic>,
        communicator: PolkadotVaultVariantSignCommunicator,
        resourceManager: ResourceManager,
        polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
        signingNotSupportedPresentable: SigningNotSupportedPresentable
    ) = PolkadotVaultSigner(
        signingSharedState = signingSharedState,
        signFlowRequester = communicator,
        resourceManager = resourceManager,
        polkadotVaultVariantConfigProvider = polkadotVaultVariantConfigProvider,
        messageSigningNotSupported = signingNotSupportedPresentable
    )

    @Provides
    @FeatureScope
    fun provideLedgerSigner(
        signingSharedState: MutableSharedState<SignerPayloadExtrinsic>,
        communicator: LedgerSignCommunicator,
        resourceManager: ResourceManager,
        signingNotSupportedPresentable: SigningNotSupportedPresentable
    ) = LedgerSigner(signingSharedState, communicator, resourceManager, signingNotSupportedPresentable)

    @Provides
    @FeatureScope
    fun provideSignerProvider(
        secretsSignerFactory: SecretsSignerFactory,
        proxiedSignerFactory: ProxiedSignerFactory,
        watchOnlySigner: WatchOnlySigner,
        paritySignerSigner: ParitySignerSigner,
        polkadotVaultSigner: PolkadotVaultSigner,
        proxiedFeeSignerFactory: ProxiedFeeSignerFactory,
        ledgerSigner: LedgerSigner
    ): SignerProvider = RealSignerProvider(
        secretsSignerFactory = secretsSignerFactory,
        watchOnlySigner = watchOnlySigner,
        paritySignerSigner = paritySignerSigner,
        polkadotVaultSigner = polkadotVaultSigner,
        proxiedSignerFactory = proxiedSignerFactory,
        proxiedFeeSignerFactory = proxiedFeeSignerFactory,
        ledgerSigner = ledgerSigner
    )
}
