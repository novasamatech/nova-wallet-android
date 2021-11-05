package io.novafoundation.nova.common.utils

class Union<L, R> private constructor(
    @PublishedApi
    internal val value: Any?,
    val isLeft: Boolean
) {

    val isRight get() = !isLeft

    companion object {

        fun <L, R> left(value: L): Union<L, R> = Union(value, isLeft = true)

        fun <L, R> right(value: R): Union<L, R> = Union(value, isLeft = false)
    }

    fun leftOrNull() = if (isLeft) {
        value as L
    } else {
        null
    }

    fun rightOrNull() = if (isRight) {
        value as R
    } else {
        null
    }
}

fun <L, R> Union<L, R>.leftOrThrow() = leftOrNull() ?: error("Not a left value")
fun <L, R> Union<L, R>.rightOrThrow() = rightOrNull() ?: error("Not a right value")

inline fun <L, R, T> Union<L, R>.fold(
    left: (L) -> T,
    right: (R) -> T
): T {
    return if (isLeft) {
        left(leftOrThrow())
    } else {
        right(rightOrThrow())
    }
}
