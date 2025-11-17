package io.novafoundation.nova.feature_account_api.presenatation.mixin.common

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface SelectWalletFilterPayload : Parcelable {
    @Parcelize
    object Everything : SelectWalletFilterPayload

    @Parcelize
    object ControllableWallets : SelectWalletFilterPayload

    @Parcelize
    class ExcludeMetaIds(val metaIds: List<Long>) : SelectWalletFilterPayload
}
