package io.novafoundation.nova.web3names.domain.caip19

fun String.splitToNamespaces(): Pair<String, String> {
    val (chainNamespace, tokenNamespace) = split("/")
    return Pair(chainNamespace, tokenNamespace)
}

fun String.toNamespaceAndReference(): Pair<String, String> {
    val (namespaceName, namespaceReference) = split(":")
    return Pair(namespaceName, namespaceReference)
}
