package io.novafoundation.nova.feature_account_impl.di.modules.signers

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.DefaultMutableSharedState
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.data.signer.SigningSharedState
import io.novafoundation.nova.feature_account_impl.data.signer.RealSignerProvider
import io.novafoundation.nova.feature_account_impl.data.signer.derivative.DerivativeSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.ledger.LedgerSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.multisig.MultisigSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.PolkadotVaultVariantSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.ProxiedSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.secrets.SecretsSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.watchOnly.WatchOnlySignerFactory

@Module(includes = [ProxiedSignerModule::class, MultisigSignerModule::class])
class SignersModule {

    @Provides
    @FeatureScope
    fun provideSignSharedState(): SigningSharedState = DefaultMutableSharedState()

    @Provides
    @FeatureScope
    fun provideSignerProvider(
        secretsSignerFactory: SecretsSignerFactory,
        proxiedSignerFactory: ProxiedSignerFactory,
        watchOnlySignerFactory: WatchOnlySignerFactory,
        polkadotVaultSignerFactory: PolkadotVaultVariantSignerFactory,
        ledgerSignerFactory: LedgerSignerFactory,
        multisigSignerFactory: MultisigSignerFactory,
        derivativeSignerFactory: DerivativeSignerFactory,
    ): SignerProvider = RealSignerProvider(
        secretsSignerFactory = secretsSignerFactory,
        watchOnlySigner = watchOnlySignerFactory,
        polkadotVaultSignerFactory = polkadotVaultSignerFactory,
        proxiedSignerFactory = proxiedSignerFactory,
        ledgerSignerFactory = ledgerSignerFactory,
        multisigSignerFactory = multisigSignerFactory,
        derivativeSignerFactory = derivativeSignerFactory
    )
}
