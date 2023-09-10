package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SubstrateAssetExistenceValidation<P, E>(
    private val assetRepository: ChainAssetRepository,
    private val chain: (P) -> Chain,
    private val tokenId: (P) -> String,
    private val assetNotExistError: AssetNotExistError<E>,
    private val addressMappingError: (P) -> E,
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        return try {
            val alreadyExistingSymbol = assetRepository.getAssetSymbolByTypeExtras(
                chainId = chain(value).id,
                assetId = tokenId(value)
            )

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

fun <P, E> ValidationSystemBuilder<P, E>.substrateAssetNotExist(
    assetRepository: ChainAssetRepository,
    chain: (P) -> Chain,
    tokenId: (P) -> String,
    assetNotExistError: AssetNotExistError<E>,
    addressMappingError: (P) -> E
) = validate(
    SubstrateAssetExistenceValidation(
        assetRepository = assetRepository,
        chain = chain,
        tokenId = tokenId,
        assetNotExistError = assetNotExistError,
        addressMappingError = addressMappingError
    )
)
