package io.novafoundation.nova.common.utils.formatting

class TimerValue(
    val millis: Long,
    val millisCalculatedAt: Long? = null, // used to offset timer value if timer is rerun, e.g. in the RecyclerView
)
