package io.novafoundation.nova.feature_account_api.data.signer

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.Signer

interface SignerProvider {

    fun signerFor(metaAccount: MetaAccount): Signer
}
