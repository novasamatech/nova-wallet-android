package io.novafoundation.nova.feature_assets.presentation.transaction.detail.extrinsic.model

import io.novafoundation.nova.common.address.AddressModel

class ExtrinsicContentModel(val blocks: List<Block>) {

    class Block(val entries: List<BlockEntry>)

    sealed class BlockEntry {

        class TransactionId(val label: String, val hash: String) : BlockEntry()

        class Address(val label: String, val addressModel: AddressModel) : BlockEntry()

        class LabeledValue(val label: String, val value: String) : BlockEntry()
    }
}
