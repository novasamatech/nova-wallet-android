package io.novafoundation.nova.feature_nft_impl.presentation.nft.common.exception

import io.novafoundation.nova.feature_nft_api.data.model.Nft
import java.lang.RuntimeException

class NftTypeNotSupportedException(type: Nft.Type): RuntimeException("Nft type (${type}) not supported")
