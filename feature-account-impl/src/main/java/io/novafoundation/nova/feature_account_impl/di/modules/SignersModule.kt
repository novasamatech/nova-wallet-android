package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.MutableSharedState
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.presenatation.account.watchOnly.WatchOnlyMissingKeysPresenter
import io.novafoundation.nova.feature_account_impl.data.signer.RealSignerProvider
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.ParitySignerSigner
import io.novafoundation.nova.feature_account_impl.data.signer.secrets.SecretsSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.watchOnly.WatchOnlySigner
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.notSupported.ParitySignerSigningNotSupportedPresentable
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic

@Module
class SignersModule {

    @Provides
    @FeatureScope
    fun provideSecretsSignerFactory(secretStoreV2: SecretStoreV2) = SecretsSignerFactory(secretStoreV2)

    @Provides
    @FeatureScope
    fun provideWatchOnlySigner(
        watchOnlySigningPresenter: WatchOnlyMissingKeysPresenter
    ) = WatchOnlySigner(watchOnlySigningPresenter)

    @Provides
    @FeatureScope
    fun provideParitySignerSigner(
        signingSharedState: MutableSharedState<SignerPayloadExtrinsic>,
        communicator: ParitySignerSignInterScreenCommunicator,
        signingNotSupportedPresentable: ParitySignerSigningNotSupportedPresentable
    ) = ParitySignerSigner(signingSharedState, communicator, signingNotSupportedPresentable)

    @Provides
    @FeatureScope
    fun provideSignerProvider(
        secretsSignerFactory: SecretsSignerFactory,
        watchOnlySigner: WatchOnlySigner,
        paritySignerSigner: ParitySignerSigner
    ): SignerProvider = RealSignerProvider(secretsSignerFactory, watchOnlySigner, paritySignerSigner)
}
