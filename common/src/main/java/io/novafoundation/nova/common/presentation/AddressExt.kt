package io.novafoundation.nova.common.presentation

import io.novafoundation.nova.common.utils.ellipsizeMiddle

fun String.ellipsizeAddress(): String {
    return ellipsizeMiddle(5).toString()
}
