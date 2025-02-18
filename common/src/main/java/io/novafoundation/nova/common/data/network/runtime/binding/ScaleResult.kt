package io.novafoundation.nova.common.data.network.runtime.binding

sealed class ScaleResult<out T, out E> {

    class Ok<T>(val value: T) : ScaleResult<T, Nothing>()

    class Error<E>(val error: E) : ScaleResult<Nothing, E>()

    companion object {

        fun <T, E> bind(
            dynamicInstance: Any?,
            bindOk: (Any?) -> T,
            bindError: (Any?) -> E
        ): ScaleResult<T, E> {
            val asEnum = dynamicInstance.castToDictEnum()

            return when (asEnum.name) {
                "Ok" -> Ok(bindOk(asEnum.value))
                "Error" -> Error(bindError(asEnum.value))
                else -> error("Unknown Result variant: ${asEnum.name}")
            }
        }
    }
}
