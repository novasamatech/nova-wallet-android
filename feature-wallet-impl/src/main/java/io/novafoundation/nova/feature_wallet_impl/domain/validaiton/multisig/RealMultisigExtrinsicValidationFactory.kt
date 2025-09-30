package io.novafoundation.nova.feature_wallet_impl.domain.validaiton.multisig

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.multisig.repository.MultisigValidationsRepository
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationBuilder
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.validation.MultisigExtrinsicValidationFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import javax.inject.Inject

@FeatureScope
class RealMultisigExtrinsicValidationFactory @Inject constructor(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val extrinsicService: ExtrinsicService,
    private val multisigValidationsRepository: MultisigValidationsRepository,
    private val chainRegistry: ChainRegistry,
) : MultisigExtrinsicValidationFactory {

    context(MultisigExtrinsicValidationBuilder)
    override fun multisigSignatoryHasEnoughBalance() {
        validate(MultisigSignatoryHasEnoughBalanceValidation(assetSourceRegistry, extrinsicService, multisigValidationsRepository))
    }

    context(MultisigExtrinsicValidationBuilder)
    override fun noPendingMultisigWithSameCallData() {
        validate(MultisigNoPendingCallHashValidation(chainRegistry, multisigValidationsRepository))
    }
}
