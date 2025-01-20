package io.novafoundation.nova.common.utils.scale

interface ToDynamicScaleInstance {

    fun toEncodableInstance(): Any?
}

@JvmInline
value class DynamicScaleInstance(val value: Any?) : ToDynamicScaleInstance {

    override fun toEncodableInstance(): Any? {
        return value
    }
}
