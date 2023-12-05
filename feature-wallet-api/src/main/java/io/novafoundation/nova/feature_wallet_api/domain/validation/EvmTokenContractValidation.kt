package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.address.format.EthereumAddressFormat
import io.novafoundation.nova.common.address.format.asAddress
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.runtime.ethereum.contract.base.querySingle
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Standard
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getCallEthereumApiOrThrow
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class EvmTokenContractValidation<P, E>(
    private val ethereumAddressFormat: EthereumAddressFormat,
    private val erc20Standard: Erc20Standard,
    private val chainRegistry: ChainRegistry,
    private val chain: (P) -> Chain,
    private val address: (P) -> String,
    private val error: (P) -> E,
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val evmAddress = address(value).asAddress()
        val isEvmAddress = ethereumAddressFormat.isValidAddress(evmAddress)
        return if (isEvmAddress) {
            isTokenContract(value).isTrueOrError { error(value) }
        } else {
            validationError(error(value))
        }
    }

    private suspend fun isTokenContract(value: P): Boolean {
        val ethApi = chainRegistry.getCallEthereumApiOrThrow(chain(value).id)
        return try {
            erc20Standard.querySingle(address(value), ethApi)
                .totalSupply()
            true
        } catch (e: Exception) {
            false
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.validErc20Contract(
    ethereumAddressFormat: EthereumAddressFormat,
    erc20Standard: Erc20Standard,
    chainRegistry: ChainRegistry,
    chain: (P) -> Chain,
    address: (P) -> String,
    error: (P) -> E,
) = validate(
    EvmTokenContractValidation(
        ethereumAddressFormat = ethereumAddressFormat,
        erc20Standard = erc20Standard,
        chainRegistry = chainRegistry,
        chain = chain,
        address = address,
        error = error
    )
)
