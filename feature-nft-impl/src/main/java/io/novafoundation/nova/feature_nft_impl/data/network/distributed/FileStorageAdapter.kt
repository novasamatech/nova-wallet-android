package io.novafoundation.nova.feature_nft_impl.data.network.distributed

enum class FileStorage(val prefix: String, val defaultHttpsGateway: String?) {
    IPFS("ipfs://ipfs/", "https://image.w.kodadot.xyz/ipfs/"),
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

    fun String.adoptFileStorageLinkToHttps() = adaptToHttps(this)

    fun adaptToHttps(distributedStorageLink: String): String {
        val distributedStorage = FileStorage.values().firstOrNull { storage ->
            distributedStorageLink.pointsTo(storage)
        } ?: FileStorage.IPFS

        val gateway = distributedStorage.defaultHttpsGateway ?: return distributedStorageLink

        validateHttpsGateway(gateway)

        val path = distributedStorageLink.removePrefix(distributedStorage.prefix)

        return "$gateway$path"
    }

    private fun String.pointsTo(fileStorage: FileStorage) = startsWith(fileStorage.prefix)
}
