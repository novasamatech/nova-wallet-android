package io.novafoundation.nova.feature_account_impl.presentation.exporting

import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.parcelize.Parcelize

sealed interface ExportPayload : Parcelable {

    val metaId: Long

    @Parcelize
    data class MetaAccount(
        override val metaId: Long
    ) : ExportPayload

    @Parcelize
    data class ChainAccount(
        override val metaId: Long,
        val chainId: ChainId,
    ) : ExportPayload
}
