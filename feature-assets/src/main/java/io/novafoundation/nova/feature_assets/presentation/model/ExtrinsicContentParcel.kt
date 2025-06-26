package io.novafoundation.nova.feature_assets.presentation.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class ExtrinsicContentParcel(val blocks: List<Block>) : Parcelable {

    @Parcelize
    class Block(val entries: List<BlockEntry>) : Parcelable

    sealed class BlockEntry : Parcelable {

        @Parcelize
        class TransactionId(val hash: String) : BlockEntry()

        @Parcelize
        class Address(val label: String, val address: String) : BlockEntry()

        @Parcelize
        class LabeledValue(val label: String, val value: String) : BlockEntry()
    }
}

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class ExtrinsicContentDsl

interface ExtrinsicContentParcelBuilder {

    interface BlockBuilder {

        fun transactionId(hash: String)

        fun address(label: String, address: String)

        fun value(label: String, value: String)
    }

    fun block(building: (@ExtrinsicContentDsl BlockBuilder).() -> Unit)

    fun build(): ExtrinsicContentParcel
}

fun ExtrinsicContentParcel(building: (@ExtrinsicContentDsl ExtrinsicContentParcelBuilder).() -> Unit): ExtrinsicContentParcel {
    return RealExtrinsicContentParcelBuilder().apply(building).build()
}

private class RealExtrinsicContentParcelBuilder : ExtrinsicContentParcelBuilder {

    private val blocks = mutableListOf<ExtrinsicContentParcel.Block>()

    override fun block(building: ExtrinsicContentParcelBuilder.BlockBuilder.() -> Unit) {
        blocks += BlockBuilder().apply(building).build()
    }

    override fun build(): ExtrinsicContentParcel {
        return ExtrinsicContentParcel(blocks)
    }
}

private class BlockBuilder : ExtrinsicContentParcelBuilder.BlockBuilder {

    private val entries = mutableListOf<ExtrinsicContentParcel.BlockEntry>()

    override fun transactionId(hash: String) {
        entries += ExtrinsicContentParcel.BlockEntry.TransactionId(hash)
    }

    override fun address(label: String, address: String) {
        entries += ExtrinsicContentParcel.BlockEntry.Address(label, address)
    }

    override fun value(label: String, value: String) {
        entries += ExtrinsicContentParcel.BlockEntry.LabeledValue(label, value)
    }

    fun build(): ExtrinsicContentParcel.Block {
        return ExtrinsicContentParcel.Block(entries)
    }
}
