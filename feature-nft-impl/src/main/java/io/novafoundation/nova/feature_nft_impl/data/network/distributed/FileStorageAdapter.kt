package io.novafoundation.nova.feature_nft_impl.data.network.distributed

enum class FileStorage(val prefix: String, val additionalPaths: List<String>, val defaultHttpsGateway: String?) {
    IPFS("ipfs://", listOf("ipfs/"), "https://image.w.kodadot.xyz/ipfs/"),
    HTTPS("https://", emptyList(), null),
    HTTP("http://", emptyList(), null);

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

        var path = distributedStorageLink.removePrefix(distributedStorage.prefix)
        distributedStorage.additionalPaths.forEach {
            path = path.removePrefix(it)
        }

        return "$gateway$path"
    }

    private fun String.pointsTo(fileStorage: FileStorage) = startsWith(fileStorage.prefix)
}
