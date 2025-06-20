package io.novafoundation.nova.feature_account_api.data.signer

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

interface SignerProvider {

    fun rootSignerFor(metaAccount: MetaAccount): NovaSigner

    fun nestedSignerFor(metaAccount: MetaAccount): NovaSigner
}
