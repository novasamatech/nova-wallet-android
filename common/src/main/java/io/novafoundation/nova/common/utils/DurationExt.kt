package io.novafoundation.nova.common.utils

import kotlin.time.Duration

inline val Duration.lastDays: Long
    get() = this.inWholeDays

val Duration.lastHours: Int
    get() = this.toComponents { _, hours, _, _, _ -> hours }

val Duration.lastMinutes: Int
    get() = this.toComponents { _, _, minutes, _, _ -> minutes }

val Duration.lastSeconds: Int
    get() = this.toComponents { _, _, _, seconds, _ -> seconds }
