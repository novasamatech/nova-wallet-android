package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.castToList

data class UnappliedSlashKey(val validator: AccountIdKey) {

    companion object {

        fun bind(decoded: Any?): UnappliedSlashKey {
            val (validator) = decoded.castToList()

            return UnappliedSlashKey(
                validator = bindAccountIdKey(validator),
            )
        }
    }
}
