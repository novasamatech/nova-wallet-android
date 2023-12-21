package io.novafoundation.nova.feature_account_impl.data.signer

import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_impl.data.signer.ledger.LedgerSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.PolkadotVaultVariantSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.ProxiedFeeSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.ProxiedSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.secrets.SecretsSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.watchOnly.WatchOnlySignerFactory
import io.novafoundation.nova.runtime.extrinsic.signer.DefaultFeeSigner
import io.novafoundation.nova.runtime.extrinsic.signer.NovaSigner
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

internal class RealSignerProvider(
    private val secretsSignerFactory: SecretsSignerFactory,
    private val proxiedSignerFactory: ProxiedSignerFactory,
    private val watchOnlySigner: WatchOnlySignerFactory,
    private val polkadotVaultSignerFactory: PolkadotVaultVariantSignerFactory,
    private val proxiedFeeSignerFactory: ProxiedFeeSignerFactory,
    private val ledgerSignerFactory: LedgerSignerFactory,
) : SignerProvider {

    override fun signerFor(metaAccount: MetaAccount): NovaSigner {
        return when (metaAccount.type) {
            LightMetaAccount.Type.SECRETS -> secretsSignerFactory.create(metaAccount)
            LightMetaAccount.Type.WATCH_ONLY -> watchOnlySigner.create(metaAccount)
            LightMetaAccount.Type.PARITY_SIGNER -> polkadotVaultSignerFactory.createParitySigner(metaAccount)
            LightMetaAccount.Type.POLKADOT_VAULT -> polkadotVaultSignerFactory.createPolkadotVault(metaAccount)
            LightMetaAccount.Type.LEDGER -> ledgerSignerFactory.create(metaAccount)
            LightMetaAccount.Type.PROXIED -> proxiedSignerFactory.create(metaAccount, this)
        }
    }

    override fun feeSigner(metaAccount: MetaAccount, chain: Chain): NovaSigner {
        return when (metaAccount.type) {
            LightMetaAccount.Type.SECRETS,
            LightMetaAccount.Type.WATCH_ONLY,
            LightMetaAccount.Type.PARITY_SIGNER,
            LightMetaAccount.Type.POLKADOT_VAULT,
            LightMetaAccount.Type.LEDGER -> DefaultFeeSigner(chain)

            LightMetaAccount.Type.PROXIED -> proxiedFeeSignerFactory.create(metaAccount, chain, this)
        }
    }
}
