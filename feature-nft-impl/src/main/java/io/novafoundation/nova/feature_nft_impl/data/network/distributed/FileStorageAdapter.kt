package io.novafoundation.nova.feature_nft_impl.data.network.distributed

enum class FileStorage(val protocol: String, val defaultHttpsGateway: String?) {
    IPFS("ipfs", "https://cloudflare-ipfs.com"),
    HTTPS("https", null),
    HTTP("http", null);

    init {
        require(!defaultHttpsGateway.orEmpty().endsWith("/")) {
            "Gateway should not end with '/' separator"
        }
        require(!protocol.endsWith("://")) {
            "Protocol should not end with '://' separator"
        }
    }
}

val FileStorage.protocolPrefix
    get() = "$protocol://"

object FileStorageAdapter {


    fun adaptToHttps(
        distributedStorageLink: String,
        customGateways: Map<FileStorage, String> = emptyMap()
    ): String? {
        val distributedStorage = FileStorage.values().firstOrNull { storage ->
            distributedStorageLink.pointsTo(storage)
        } ?: return null

        val gateway = customGateways[distributedStorage] ?: distributedStorage.defaultHttpsGateway
            ?: return distributedStorageLink

        val path = distributedStorageLink.removePrefix(distributedStorage.protocolPrefix)

        return "$gateway/$path"
    }

    private fun String.pointsTo(fileStorage: FileStorage) = startsWith(fileStorage.protocolPrefix)
}
