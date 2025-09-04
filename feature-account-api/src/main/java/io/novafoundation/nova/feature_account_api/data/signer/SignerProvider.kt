package io.novafoundation.nova.feature_account_api.data.signer

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.extrinsic.signer.FeeSigner
import io.novafoundation.nova.runtime.extrinsic.signer.NovaSigner
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface SignerProvider {

    fun rootSignerFor(metaAccount: MetaAccount): NovaSigner

    fun nestedSignerFor(metaAccount: MetaAccount): NovaSigner

    fun feeSigner(metaAccount: MetaAccount, chain: Chain): FeeSigner
}
