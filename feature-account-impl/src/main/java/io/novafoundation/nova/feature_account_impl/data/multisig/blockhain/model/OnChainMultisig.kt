package io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_account_api.data.multisig.model.MultisigTimePoint
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import java.math.BigInteger

class OnChainMultisig(
    val callHash: CallHash,
    val approvals: List<AccountIdKey>,
    val deposit: BigInteger,
    val depositor: AccountIdKey,
    val timePoint: MultisigTimePoint,
) {

    companion object {

        fun bind(decoded: Any?, callHash: CallHash): OnChainMultisig {
            val struct = decoded.castToStruct()

            return OnChainMultisig(
                callHash = callHash,
                approvals = bindList(struct["approvals"], ::bindAccountIdKey),
                deposit = bindNumber(struct["deposit"]),
                depositor = bindAccountIdKey(struct["depositor"]),
                timePoint = MultisigTimePoint.bind(struct["when"])
            )
        }
    }
}
