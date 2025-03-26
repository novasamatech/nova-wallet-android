package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2

import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class FeeContext(
    /**
     *  Logical asset of the operation. For example, when sending USDT and paying fee in DOT,
     *  operationAsset will be USDT
     */
    val operationAsset: Chain.Asset,
    /**
     *  Utility asset of the logical of the operation. For example, when sending USDT on Hydration,
     *  operationChainUtilityAsset will be HDX
     */
    val operationChainUtilityAssetSource: OperationUtilityAssetSource
) {

    /**
     * Determines how [FeeLoaderMixinV2] should detect utility asset for the chain.
     * This utility asset is used as a default fee payment asset
     */
    sealed interface OperationUtilityAssetSource {

        /**
         * Utility asset should be detected based on the chain of the [FeeContext.operationAsset]
         * This source can be used when chain of the [FeeContext.operationAsset] is known to Nova,
         * i.e. it is present in [ChainRegistry]
         */
        object DetectFromOperationChain : OperationUtilityAssetSource

        /**
         * The specified [operationChainUtilityAsset] should be used as utility asset
         * This mode is usefull when we cannot provide guarantees that a [FeeContext.operationAsset] is present in ChainRegistry
         *
         * This might be the case for some logic that construct [Chain.Asset] on the fly to use components such as [FeeLoaderMixinV2]
         * For example, Dapp Browser tx signing flow might do so for unknown EVM chains
         */
        class Specified(val operationChainUtilityAsset: Chain.Asset) : OperationUtilityAssetSource
    }
}
