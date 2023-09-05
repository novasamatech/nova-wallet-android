package io.novafoundation.nova.feature_nft_impl.domain.validaiton

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import kotlinx.coroutines.flow.first

class NftExistsValidation<P, E>(
    private val nftDetails: suspend (nftId: String) -> NftDetails,
    private val substratePublicKey: (P) -> ByteArray,
    private val nftId: (P) -> String,
    private val error: () -> E
): Validation<P, E> {

    companion object;

    override suspend fun validate(value: P): ValidationStatus<E> {
        val nftId = nftId(value)
        val nftOwnerAccountId = nftDetails(nftId).owner
        val substratePublicKey = substratePublicKey(value)
        if (nftOwnerAccountId.toHexString() != substratePublicKey.toHexString()) {
            return validationError(error())
        }
        return ValidationStatus.Valid()
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.nftExists(
    nftDetails: suspend (nftId: String) -> NftDetails,
    substratePublicKey: (P) -> ByteArray,
    nftId: (P) -> String,
    error: () -> E
) = validate(
    NftExistsValidation(nftDetails, substratePublicKey, nftId, error)
)
