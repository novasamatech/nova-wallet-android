package io.novafoundation.nova.feature_nft_impl.data.source.providers.pdc20.network

import java.math.BigDecimal
import java.math.BigInteger

class Pdc20NftResponse(
    val userTokenBalances: List<Pdc20NftRemote>,
    val listings: List<Pdc20Listing>
)

class Pdc20NftRemote(
    val balance: String,
    val address: PdcAddress,
    val token: Token
) {

    class Token(
        val id: String,
        val logo: String?,
        val ticker: String?,
        val totalSupply: String?,
        val network: String
    )
}

class Pdc20Listing(
    val from: PdcAddress,
    val token: Token,
    val amount: BigInteger,
    val value: BigDecimal
) {

    class Token(
        val id: String
    )
}

class PdcAddress(val address: String)

class RmrkV1NftMetadataRemote(
    val image: String,
    val description: String
)
