package io.novafoundation.nova.feature_account_impl.data.signer

import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_impl.data.signer.secrets.SecretsSignerFactory
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.Signer

internal class RealSignerProvider(
    private val secretsSignerFactory: SecretsSignerFactory,
): SignerProvider {

    override fun signerFor(metaAccount: MetaAccount): Signer {
        return secretsSignerFactory.create(metaAccount)
    }
}
