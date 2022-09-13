package io.novafoundation.nova.common.utils

@JvmInline
value class ComponentHolder(val values: List<*>) {
    inline operator fun <reified T> component1() = values[0] as T
    inline operator fun <reified T> component2() = values[1] as T
    inline operator fun <reified T> component3() = values[2] as T
    inline operator fun <reified T> component4() = values[3] as T
    inline operator fun <reified T> component5() = values[4] as T
}
