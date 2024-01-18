package io.novafoundation.nova.app.root.presentation.requestBusHandler

class CompoundRequestBusHandler(
    private val handlers: Set<RequestBusHandler>
) : RequestBusHandler {
    override fun observe() {
        handlers.forEach { it.observe() }
    }
}
