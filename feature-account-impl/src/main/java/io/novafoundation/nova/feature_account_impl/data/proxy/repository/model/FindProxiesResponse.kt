package io.novafoundation.nova.feature_account_impl.data.proxy.repository.model

import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes
import io.novafoundation.nova.common.utils.HexString

class FindMultisigsResponse(
    val accounts: SubQueryNodes<MultisigRemote>
)

class MultisigRemote(
    val id: HexString,
    val threshold: Int,
    val signatories: SubQueryNodes<SignatoryRemoteWrapper>
) {

    class SignatoryRemoteWrapper(val signatory: SignatoryRemote)

    class SignatoryRemote(
        val id: HexString
    )
}
