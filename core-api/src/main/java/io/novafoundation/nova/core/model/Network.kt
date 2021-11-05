package io.novafoundation.nova.core.model

data class Network(
    val type: Node.NetworkType
) {
    val name = type.readableName
}
