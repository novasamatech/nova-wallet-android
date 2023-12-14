package io.novafoundation.nova.runtime.extrinsic.feeSigner

import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.Signer

interface FeeSigner : Signer {

    suspend fun accountId(): AccountId
}
