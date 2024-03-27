package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.common.validation.validationWarning
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.assetOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.chainAssetIdOfErc20Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.Source
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

typealias AssetNotExistError<E> = (existingSymbol: String, canModify: Boolean) -> E

class EvmAssetExistenceValidation<P, E>(
    private val chainRegistry: ChainRegistry,
    private val chain: (P) -> Chain,
    private val contractAddress: (P) -> String,
    private val assetAlreadyExists: AssetNotExistError<E>,
    private val addressMappingError: (P) -> E,
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        return try {
            val assetId = chainAssetIdOfErc20Token(contractAddress(value))
            val fullAssetId = FullChainAssetId(chain(value).id, assetId)
            val alreadyExistingAsset = chainRegistry.assetOrNull(fullAssetId)

            when {
                alreadyExistingAsset == null -> valid()
                // we only allow to modify manually added tokens. Default tokens should remain unchanged
                alreadyExistingAsset.source == Source.MANUAL -> assetAlreadyExists(alreadyExistingAsset.symbol.value, true).validationWarning()
                else -> assetAlreadyExists(alreadyExistingAsset.symbol.value, false).validationError()
            }
        } catch (e: Exception) {
            validationError(addressMappingError(value))
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.evmAssetNotExists(
    chainRegistry: ChainRegistry,
    chain: (P) -> Chain,
    address: (P) -> String,
    assetNotExistError: AssetNotExistError<E>,
    addressMappingError: (P) -> E
) = validate(
    EvmAssetExistenceValidation(
        chainRegistry = chainRegistry,
        chain = chain,
        contractAddress = address,
        assetAlreadyExists = assetNotExistError,
        addressMappingError = addressMappingError
    )
)
