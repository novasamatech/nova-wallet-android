package io.novafoundation.nova.common.utils.recyclerView

@JvmInline
value class ViewType(val value: Int)

fun Int.asViewType() = ViewType(this)
