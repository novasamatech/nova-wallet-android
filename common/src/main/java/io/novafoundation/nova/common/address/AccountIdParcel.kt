package io.novafoundation.nova.common.address

import android.os.Parcelable
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.android.parcel.Parcelize

@Parcelize
class AccountIdParcel(private val value: AccountId) : Parcelable {

    companion object {

        fun fromHex(hexAccountId: String): AccountIdParcel {
            return AccountIdParcel(hexAccountId.fromHex())
        }
    }

    val accountId: AccountIdKey
        get() = value.intoKey()
}
