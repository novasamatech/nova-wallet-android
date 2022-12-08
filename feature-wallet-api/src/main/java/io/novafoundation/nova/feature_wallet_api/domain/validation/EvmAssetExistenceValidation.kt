package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.chainAssetIdOfErc20Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

typealias AssetNotExistError<E> = (existingSymbol: String) -> E

class EvmAssetExistenceValidation<P, E>(
    private val assetRepository: ChainAssetRepository,
    private val chain: (P) -> Chain,
    private val contractAddress: (P) -> String,
    private val assetNotExistError: AssetNotExistError<E>,
    private val addressMappingError: (P) -> E,
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        return try {
            val assetId = chainAssetIdOfErc20Token(contractAddress(value))
            val fullAssetId = FullChainAssetId(chain(value).id, assetId)
            val alreadyExistingSymbol = assetRepository.getAssetSymbol(fullAssetId)

            if (alreadyExistingSymbol != null) {
                validationError(assetNotExistError(alreadyExistingSymbol))
            } else {
                valid()
            }
        } catch (e: Exception) {
            validationError(addressMappingError(value))
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.evmAssetNotExist(
    assetRepository: ChainAssetRepository,
    chain: (P) -> Chain,
    address: (P) -> String,
    assetNotExistError: AssetNotExistError<E>,
    addressMappingError: (P) -> E
) = validate(
    EvmAssetExistenceValidation(
        assetRepository = assetRepository,
        chain = chain,
        contractAddress = address,
        assetNotExistError = assetNotExistError,
        addressMappingError = addressMappingError
    )
)
