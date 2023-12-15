package io.novafoundation.nova.feature_account_api.data.signer

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.extrinsic.feeSigner.FeeSigner
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.Signer

interface SignerProvider {

    fun signerFor(metaAccount: MetaAccount): Signer

    fun feeSigner(chain: Chain): FeeSigner

    fun feeSigner(metaAccount: MetaAccount, chain: Chain): FeeSigner
}
