package io.novafoundation.nova.feature_account_impl.data.signer

import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_impl.data.signer.ledger.LedgerFeeSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.ledger.LedgerSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.PolkadotVaultVariantSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.ProxiedFeeSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.ProxiedSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.secrets.SecretsSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.watchOnly.WatchOnlySignerFactory
import io.novafoundation.nova.runtime.extrinsic.signer.FeeSigner
import io.novafoundation.nova.runtime.extrinsic.signer.NovaSigner
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

internal class RealSignerProvider(
    private val secretsSignerFactory: SecretsSignerFactory,
    private val proxiedSignerFactory: ProxiedSignerFactory,
    private val watchOnlySigner: WatchOnlySignerFactory,
    private val polkadotVaultSignerFactory: PolkadotVaultVariantSignerFactory,
    private val proxiedFeeSignerFactory: ProxiedFeeSignerFactory,
    private val ledgerSignerFactory: LedgerSignerFactory,
    private val ledgerFeeSignerFactory: LedgerFeeSignerFactory,
) : SignerProvider {

    override fun rootSignerFor(metaAccount: MetaAccount): NovaSigner {
        return signerFor(metaAccount, isRoot = true)
    }

    override fun nestedSignerFor(metaAccount: MetaAccount): NovaSigner {
        return signerFor(metaAccount, isRoot = false)
    }

    override fun feeSigner(metaAccount: MetaAccount, chain: Chain): FeeSigner {
        return when (metaAccount.type) {
            LightMetaAccount.Type.SECRETS,
            LightMetaAccount.Type.WATCH_ONLY,
            LightMetaAccount.Type.PARITY_SIGNER,
            LightMetaAccount.Type.POLKADOT_VAULT -> DefaultFeeSigner(metaAccount, chain)

            LightMetaAccount.Type.LEDGER,
            LightMetaAccount.Type.LEDGER_LEGACY -> ledgerFeeSignerFactory.create(metaAccount, chain)

            LightMetaAccount.Type.PROXIED -> proxiedFeeSignerFactory.create(metaAccount, chain, this)
        }
    }

    private fun signerFor(metaAccount: MetaAccount, isRoot: Boolean): NovaSigner {
        return when (metaAccount.type) {
            LightMetaAccount.Type.SECRETS -> secretsSignerFactory.create(metaAccount)
            LightMetaAccount.Type.WATCH_ONLY -> watchOnlySigner.create(metaAccount)
            LightMetaAccount.Type.PARITY_SIGNER -> polkadotVaultSignerFactory.createParitySigner(metaAccount)
            LightMetaAccount.Type.POLKADOT_VAULT -> polkadotVaultSignerFactory.createPolkadotVault(metaAccount)
            LightMetaAccount.Type.LEDGER -> ledgerSignerFactory.create(metaAccount, LedgerVariant.GENERIC)
            LightMetaAccount.Type.LEDGER_LEGACY -> ledgerSignerFactory.create(metaAccount, LedgerVariant.LEGACY)
            LightMetaAccount.Type.PROXIED -> proxiedSignerFactory.create(metaAccount, this, isRoot)
        }
    }
}
