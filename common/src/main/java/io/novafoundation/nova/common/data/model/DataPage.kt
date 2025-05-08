package io.novafoundation.nova.common.data.model

import io.novafoundation.nova.common.utils.castOrNull

data class DataPage<T>(
    val nextOffset: PageOffset,
    val items: List<T>
) : List<T> by items {

    companion object {

        fun <T> empty(): DataPage<T> = DataPage(nextOffset = PageOffset.FullData, items = emptyList())
    }
}

sealed class PageOffset {

    companion object;

    sealed class Loadable : PageOffset() {
        data class Cursor(val value: String) : Loadable()

        data class PageNumber(val page: Int) : Loadable()

        object FirstPage : Loadable()
    }

    object FullData : PageOffset()
}

fun PageOffset.Companion.CursorOrFull(value: String?): PageOffset = if (value != null) {
    PageOffset.Loadable.Cursor(value)
} else {
    PageOffset.FullData
}

fun PageOffset.asCursorOrNull(): PageOffset.Loadable.Cursor? {
    return castOrNull()
}

fun PageOffset.requirePageNumber(): PageOffset.Loadable.PageNumber {
    require(this is PageOffset.Loadable.PageNumber)

    return this
}

fun PageOffset.getPageNumberOrThrow(): Int {
    return when (this) {
        PageOffset.FullData -> 0
        PageOffset.Loadable.FirstPage -> 0
        is PageOffset.Loadable.PageNumber -> page
        is PageOffset.Loadable.Cursor -> throw IllegalStateException()
    }
}
