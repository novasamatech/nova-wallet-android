package io.novafoundation.nova.runtime.extrinsic.signer

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.Signer

interface NovaSigner : Signer {

    suspend fun signerAccountId(chain: Chain): AccountId
}
