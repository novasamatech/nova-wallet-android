package io.novafoundation.nova.feature_account_api.data.signer

// TODO this can be removed if we implement comparison of tx extensions and call-data that are present in ExtrinsicBuilder
class TxModificationInfo(
    val modifiedExistingExtensions: Boolean,
    val modifiedCall: Boolean
) {

    companion object {

        fun modifiedNothing(): TxModificationInfo {
            return TxModificationInfo(modifiedExistingExtensions = false, modifiedCall = false)
        }
    }
}

val TxModificationInfo.modifiedTx: Boolean
    get() = modifiedExistingExtensions || modifiedCall

fun TxModificationInfo.andThen(otherModification: TxModificationInfo): TxModificationInfo {
    return TxModificationInfo(
        modifiedCall = modifiedCall || otherModification.modifiedCall,
        modifiedExistingExtensions = modifiedExistingExtensions || otherModification.modifiedExistingExtensions
    )
}
