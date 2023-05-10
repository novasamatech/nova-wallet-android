package io.novafoundation.nova.common.utils

import java.util.Date

fun Date.timestamp(): Long = time / 1000
