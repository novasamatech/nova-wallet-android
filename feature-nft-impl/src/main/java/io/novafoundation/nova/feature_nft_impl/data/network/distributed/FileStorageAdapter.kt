package io.novafoundation.nova.feature_nft_impl.data.network.distributed

enum class FileStorage(val prefix: String, val defaultHttpsGateway: String?) {
    IPFS("ipfs://", "https://ipfs2.rmrk.link/ipfs/"),
    HTTPS("https://", null),
    HTTP("http://", null);

    init {
        validateHttpsGateway(defaultHttpsGateway)
    }
}

private fun validateHttpsGateway(gateway: String?) {
    require(gateway == null || gateway.endsWith("/")) {
        "Gateway should end with '/' separator"
    }
}

object FileStorageAdapter {

    fun String.adoptFileStorageLinkToHttps(
        customGateways: Map<FileStorage, String> = emptyMap(),
        noProtocolStorage: FileStorage = FileStorage.IPFS
    ) = adaptToHttps(this, customGateways, noProtocolStorage)

    fun adaptToHttps(
        distributedStorageLink: String,
        customGateways: Map<FileStorage, String> = emptyMap(),
        noProtocolStorage: FileStorage = FileStorage.IPFS
    ): String {
        val distributedStorage = FileStorage.values().firstOrNull { storage ->
            distributedStorageLink.pointsTo(storage)
        } ?: noProtocolStorage

        val gateway = customGateways[distributedStorage] ?: distributedStorage.defaultHttpsGateway
            ?: return distributedStorageLink

        validateHttpsGateway(gateway)

        val path = distributedStorageLink.removePrefix(distributedStorage.prefix)

        return "$gateway$path"
    }

    private fun String.pointsTo(fileStorage: FileStorage) = startsWith(fileStorage.prefix)
}
