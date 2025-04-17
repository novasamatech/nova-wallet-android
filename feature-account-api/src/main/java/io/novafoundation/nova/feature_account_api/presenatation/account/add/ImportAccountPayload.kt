package io.novafoundation.nova.feature_account_api.presenatation.account.add

import android.os.Parcelable
import io.novafoundation.nova.feature_account_api.presenatation.account.common.model.AdvancedEncryptionModel
import kotlinx.parcelize.Parcelize

@Parcelize
class ImportAccountPayload(
    val importType: ImportType,
    val addAccountPayload: AddAccountPayload,
) : Parcelable

sealed interface ImportType : Parcelable {

    @Parcelize
    class Mnemonic(val mnemonic: String? = null, val preset: AdvancedEncryptionModel? = null) : ImportType

    @Parcelize
    object Seed : ImportType

    @Parcelize
    object Json : ImportType
}

fun SecretType.asImportType(): ImportType {
    return when (this) {
        SecretType.MNEMONIC -> ImportType.Mnemonic()
        SecretType.SEED -> ImportType.Seed
        SecretType.JSON -> ImportType.Json
    }
}
