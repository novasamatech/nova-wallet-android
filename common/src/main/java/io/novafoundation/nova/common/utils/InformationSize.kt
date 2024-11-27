package io.novafoundation.nova.common.utils

@JvmInline
value class InformationSize(val sizeInBytes: Long) : Comparable<InformationSize> {

    companion object {

        val Int.bytes: InformationSize get() = toInformationSize(InformationSizeUnit.BYTES)

        val Long.bytes: InformationSize get() = toInformationSize(InformationSizeUnit.BYTES)

        val Int.kilobytes: InformationSize get() = toInformationSize(InformationSizeUnit.KILOBYTES)

        val Long.kilobytes: InformationSize get() = toInformationSize(InformationSizeUnit.KILOBYTES)

        val Int.megabytes: InformationSize get() = toInformationSize(InformationSizeUnit.MEGABYTES)

        val Long.megabytes: InformationSize get() = toInformationSize(InformationSizeUnit.MEGABYTES)
    }

    override fun compareTo(other: InformationSize): Int {
        return sizeInBytes.compareTo(other.sizeInBytes)
    }

    operator fun plus(other: InformationSize): InformationSize {
        return InformationSize(sizeInBytes + other.sizeInBytes)
    }

    operator fun minus(other: InformationSize): InformationSize {
        return InformationSize(sizeInBytes - other.sizeInBytes)
    }
}

enum class InformationSizeUnit {

    BYTES,

    KILOBYTES,

    MEGABYTES
}

fun Int.toInformationSize(unit: InformationSizeUnit): InformationSize {
    return toLong().toInformationSize(unit)
}

fun Long.toInformationSize(unit: InformationSizeUnit): InformationSize {
    return InformationSize(unit.convertToBytes(this))
}

private fun InformationSizeUnit.convertToBytes(value: Long): Long {
    return when (this) {
        InformationSizeUnit.BYTES -> value
        InformationSizeUnit.KILOBYTES -> value * 1024
        InformationSizeUnit.MEGABYTES -> value * 1024 * 1024
    }
}
