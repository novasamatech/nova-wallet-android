package io.novafoundation.nova.caip.common

internal fun String.toNamespaceAndReference(): Pair<String, String> {
    val (namespaceName, namespaceReference) = split(":")
    return Pair(namespaceName, namespaceReference)
}
