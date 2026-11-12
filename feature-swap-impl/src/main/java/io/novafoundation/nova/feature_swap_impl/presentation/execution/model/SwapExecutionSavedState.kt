package io.novafoundation.nova.feature_swap_impl.presentation.execution.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SwapExecutionSavedState(
    val status: Status,
    val stepIndex: Int = 0,
    val at: Long = System.currentTimeMillis(),
    val reason: String? = null,
    val transactionChainId: String? = null,
    val transactionHash: String? = null,
    val transactionFailed: Boolean = false,
) : Parcelable {

    enum class Status { NEW, RUNNING, COMPLETED, FAILED }
}
