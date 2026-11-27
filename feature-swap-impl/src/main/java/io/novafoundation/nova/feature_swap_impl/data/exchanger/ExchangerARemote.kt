package io.novafoundation.nova.feature_swap_impl.data.exchanger

class ExchangerAAssetsRemote(
    val assets: List<ExchangerAAssetRemote?>?
)

/** Boxed required fields let the mapper reject missing JSON values instead of using zero or false. */
class ExchangerAAssetRemote(
    val chainId: String,
    val assetId: Int?,
    val send: Boolean?,
    val receive: Boolean?
)

class ExchangerAAssetReference(
    val chainId: String,
    val assetId: Int
)

/**
 * Amounts are plain decimal strings in token units. Factories ensure that only
 * the edited side is sent. Asset precision and rounding belong to domain mapping.
 */
class ExchangerAQuoteRequest private constructor(
    val from: ExchangerAAssetReference,
    val to: ExchangerAAssetReference,
    val payAmount: String?,
    val receiveAmount: String?
) {

    companion object {

        fun pay(from: ExchangerAAssetReference, to: ExchangerAAssetReference, amount: String): ExchangerAQuoteRequest {
            return ExchangerAQuoteRequest(from, to, payAmount = amount, receiveAmount = null)
        }

        fun receive(from: ExchangerAAssetReference, to: ExchangerAAssetReference, amount: String): ExchangerAQuoteRequest {
            return ExchangerAQuoteRequest(from, to, payAmount = null, receiveAmount = amount)
        }
    }
}

/**
 * The API discriminates successful quotes and limit responses with kind.
 * Variant-specific fields stay nullable at the transport boundary and must be
 * validated when mapped to a domain quote.
 */
class ExchangerAQuoteRemote(
    val kind: String,
    val limits: ExchangerALimitsRemote,
    val payAmount: String?,
    val receiveAmount: String?,
    val networkFees: ExchangerANetworkFeesRemote?,
    val durationMinutes: ExchangerADurationRemote?,
    val warning: String?,
    val reason: String?
)

class ExchangerALimitsRemote(
    val minPayAmount: String,
    val maxPayAmount: String?
)

class ExchangerANetworkFeesRemote(
    val deposit: String?,
    val withdrawal: String?
)

class ExchangerADurationRemote(
    val min: Int?,
    val max: Int?
)
