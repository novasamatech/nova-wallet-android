package io.novafoundation.nova.feature_account_impl.data.signer

import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_impl.data.signer.ledger.LedgerSigner
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.ParitySignerSigner
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.PolkadotVaultSigner
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.ProxiedFeeSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.ProxiedSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.secrets.SecretsSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.watchOnly.WatchOnlySigner
import io.novafoundation.nova.runtime.extrinsic.feeSigner.DefaultFeeSigner
import io.novafoundation.nova.runtime.extrinsic.feeSigner.FeeSigner
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.Signer

internal class RealSignerProvider(
    private val secretsSignerFactory: SecretsSignerFactory,
    private val proxiedSignerFactory: ProxiedSignerFactory,
    private val watchOnlySigner: WatchOnlySigner,
    private val paritySignerSigner: ParitySignerSigner,
    private val polkadotVaultSigner: PolkadotVaultSigner,
    private val proxiedFeeSignerFactory: ProxiedFeeSignerFactory,
    private val ledgerSigner: LedgerSigner,
) : SignerProvider {

    override fun signerFor(metaAccount: MetaAccount): Signer {
        return when (metaAccount.type) {
            LightMetaAccount.Type.SECRETS -> secretsSignerFactory.create(metaAccount)
            LightMetaAccount.Type.WATCH_ONLY -> watchOnlySigner
            LightMetaAccount.Type.PARITY_SIGNER -> paritySignerSigner
            LightMetaAccount.Type.POLKADOT_VAULT -> polkadotVaultSigner
            LightMetaAccount.Type.LEDGER -> ledgerSigner
            LightMetaAccount.Type.PROXIED -> proxiedSignerFactory.create(metaAccount, this)
        }
    }

    override fun feeSigner(chain: Chain): FeeSigner {
        return DefaultFeeSigner(chain)
    }

    override fun feeSigner(metaAccount: MetaAccount, chain: Chain): FeeSigner {
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
