package io.novafoundation.nova.common.base.errors

fun shouldIgnore(exception: Throwable) = exception is SigningCancelledException
