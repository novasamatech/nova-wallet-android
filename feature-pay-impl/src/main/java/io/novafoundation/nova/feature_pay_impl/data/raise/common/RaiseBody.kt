package io.novafoundation.nova.feature_pay_impl.data.raise.common

import com.google.gson.annotations.SerializedName
import io.novafoundation.nova.common.data.model.PageOffset

open class RaiseBody<D>(
    val data: D
)

open class RaiseSingleData<ATTRS>(
    val type: String,
    val attributes: ATTRS
)

typealias RaiseSingleObjectBody<ATTRS> = RaiseBody<RaiseSingleData<ATTRS>>

class RaiseListBody<D>(data: List<D>, val meta: RaiseListMeta?) : RaiseBody<List<D>>(data)

class RaiseListMeta(@SerializedName("total_count") val totalResults: Int)

fun RaiseListMeta?.toPageOffset(usedPageIndex: Int, usedPageSize: Int): PageOffset {
    return when {
        this == null -> PageOffset.FullData
        hasNextPage(usedPageIndex, usedPageSize) -> PageOffset.Loadable.PageNumber(usedPageIndex + 1)
        else -> PageOffset.FullData
    }
}

private fun RaiseListMeta.hasNextPage(usedPageIndex: Int, usedPageSize: Int): Boolean {
    // Next page start index should be in bounds
    return (usedPageIndex + 1) * usedPageSize < totalResults
}
