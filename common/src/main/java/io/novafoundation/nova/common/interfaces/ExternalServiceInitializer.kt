package io.novafoundation.nova.common.interfaces

interface ExternalServiceInitializer {
    fun initialize()
}

class CompoundExternalServiceInitializer(
    private val initializers: Set<ExternalServiceInitializer>
) : ExternalServiceInitializer {

    override fun initialize() {
        initializers.forEach { it.initialize() }
    }
}
