package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.DefaultMutableSharedState
import io.novafoundation.nova.common.utils.MutableSharedState
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.presenatation.account.watchOnly.WatchOnlyMissingKeysPresenter
import io.novafoundation.nova.feature_account_api.presenatation.sign.LedgerSignCommunicator
import io.novafoundation.nova.feature_account_impl.data.signer.RealSignerProvider
import io.novafoundation.nova.feature_account_impl.data.signer.ledger.LedgerSigner
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.ParitySignerSignCommunicator
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.ParitySignerSigner
import io.novafoundation.nova.feature_account_impl.data.signer.secrets.SecretsSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.watchOnly.WatchOnlySigner
import io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported.SigningNotSupportedPresentable
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic

@Module
class SignersModule {

    @Provides
    @FeatureScope
    fun provideSignSharedState(): MutableSharedState<SignerPayloadExtrinsic> = DefaultMutableSharedState()

    @Provides
    @FeatureScope
    fun provideSecretsSignerFactory(secretStoreV2: SecretStoreV2, chainRegistry: ChainRegistry) = SecretsSignerFactory(secretStoreV2, chainRegistry)

    @Provides
    @FeatureScope
    fun provideWatchOnlySigner(
        watchOnlySigningPresenter: WatchOnlyMissingKeysPresenter
    ) = WatchOnlySigner(watchOnlySigningPresenter)

    @Provides
    @FeatureScope
    fun provideParitySignerSigner(
        signingSharedState: MutableSharedState<SignerPayloadExtrinsic>,
        communicator: ParitySignerSignCommunicator,
        signingNotSupportedPresentable: SigningNotSupportedPresentable
    ) = ParitySignerSigner(signingSharedState, communicator, signingNotSupportedPresentable)

    @Provides
    @FeatureScope
    fun provideLedgerSigner(
        signingSharedState: MutableSharedState<SignerPayloadExtrinsic>,
        communicator: LedgerSignCommunicator,
        signingNotSupportedPresentable: SigningNotSupportedPresentable
    ) = LedgerSigner(signingSharedState, communicator, signingNotSupportedPresentable)

    @Provides
    @FeatureScope
    fun provideSignerProvider(
        secretsSignerFactory: SecretsSignerFactory,
        watchOnlySigner: WatchOnlySigner,
        paritySignerSigner: ParitySignerSigner,
        ledgerSigner: LedgerSigner
    ): SignerProvider = RealSignerProvider(
        secretsSignerFactory = secretsSignerFactory,
        watchOnlySigner = watchOnlySigner,
        paritySignerSigner = paritySignerSigner,
        ledgerSigner = ledgerSigner
    )
}
