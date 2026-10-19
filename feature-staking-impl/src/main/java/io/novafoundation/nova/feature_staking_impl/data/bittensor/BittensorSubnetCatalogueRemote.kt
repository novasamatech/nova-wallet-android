package io.novafoundation.nova.feature_staking_impl.data.bittensor

class BittensorSubnetCatalogueRemote(
    val items: List<Subnet>?,
    val meta: Meta?
) {

    class Subnet(
        val netuid: Int?,
        val taoPerAlpha: String?,
        val name: String? = null,
        val symbol: String? = null,
        val networkRegisteredAt: java.math.BigInteger? = null,
        val taoReserve: String? = null
    )

    class Meta(val components: Components?)

    class Components(val alphaPrices: BittensorRecommendationsRemote.Component?, val subnetMetadata: BittensorRecommendationsRemote.Component? = null)
}
