package io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.CoinGeckoLinkValidationFactory
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.validCoinGeckoLink
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNode.nodeSupportedByNetworkValidation
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNode.validateNetworkNodeIsAlive
import io.novafoundation.nova.runtime.ext.evmChainIdFrom
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

typealias CustomNetworkValidationSystem = ValidationSystem<CustomNetworkPayload, CustomNetworkFailure>
typealias CustomNetworkValidationSystemBuilder = ValidationSystemBuilder<CustomNetworkPayload, CustomNetworkFailure>

class CustomNetworkPayload(
    val nodeUrl: String,
    val chainName: String,
    val tokenSymbol: String,
    val evmChainId: Int?,
    val blockExplorerUrl: String?,
    val coingeckoLinkUrl: String?
)

sealed interface CustomNetworkFailure {

    class DefaultNetworkAlreadyAdded(val networkName: String) : CustomNetworkFailure

    class CustomNetworkAlreadyAdded(val networkName: String) : CustomNetworkFailure

    object WrongNetwork : CustomNetworkFailure

    object NodeIsNotAlive : CustomNetworkFailure

    object CoingeckoLinkBadFormat : CustomNetworkFailure
}

fun CustomNetworkValidationSystemBuilder.validateNetworkNodeIsAlive(
    nodeHealthStateCheckRequest: suspend (CustomNetworkPayload) -> Unit
) = validateNetworkNodeIsAlive(nodeHealthStateCheckRequest, failure = { CustomNetworkFailure.NodeIsNotAlive })

fun CustomNetworkValidationSystemBuilder.validateNodeSupportedByNetwork(
    nodeChainIdRequester: suspend (CustomNetworkPayload) -> String
) = nodeSupportedByNetworkValidation(
    nodeChainIdRequester = { nodeChainIdRequester(it) },
    originalChainId = { it.evmChainId?.let { evmChainIdFrom(it) } },
    failure = { CustomNetworkFailure.WrongNetwork }
)

fun CustomNetworkValidationSystemBuilder.validateNetworkNotAdded(
    chainRegistry: ChainRegistry,
    chainIdRequester: suspend (CustomNetworkPayload) -> String
) = validateNetworkNotAdded(
    chainRegistry = chainRegistry,
    chainIdRequester = { chainIdRequester(it) },
    defaultNetworkFailure = { payload, chain -> CustomNetworkFailure.DefaultNetworkAlreadyAdded(chain.name) },
    customNetworkWarning = { payload, chain -> CustomNetworkFailure.CustomNetworkAlreadyAdded(chain.name) }
)

fun CustomNetworkValidationSystemBuilder.validCoinGeckoLink(
    coinGeckoLinkValidationFactory: CoinGeckoLinkValidationFactory
) = validCoinGeckoLink(
    coinGeckoLinkValidationFactory = coinGeckoLinkValidationFactory,
    optional = true,
    link = { it.coingeckoLinkUrl },
    error = { CustomNetworkFailure.CoingeckoLinkBadFormat }
)
