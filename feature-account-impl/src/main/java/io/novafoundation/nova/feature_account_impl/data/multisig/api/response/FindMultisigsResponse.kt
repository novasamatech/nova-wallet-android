package io.novafoundation.nova.feature_account_impl.data.multisig.api.response

import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes
import io.novafoundation.nova.common.utils.HexString

class FindMultisigsResponse(
    val accountMultisigs: SubQueryNodes<AccountMultisigRemote>
)

class AccountMultisigRemote(
    val multisig: MultisigRemote,
) {

    class MultisigRemote(
        val accountId: HexString,
        val threshold: Int,
        val signatories: SubQueryNodes<SignatoryRemote>
    )

    class SignatoryRemote(
        val signatoryId: HexString
    )
}
