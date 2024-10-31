package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.provider

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFeeLoaderMixin
import kotlinx.coroutines.flow.Flow

class FeeLoaderProviderFactory(
    private val resourceManager: ResourceManager,
) : FeeLoaderMixin.Factory {

    override fun create(
        tokenFlow: Flow<Token?>,
        configuration: GenericFeeLoaderMixin.Configuration<Fee>
    ): FeeLoaderMixin.Presentation {
        return GenericFeeLoaderProviderPresentation(resourceManager, configuration, tokenFlow)
    }
}
