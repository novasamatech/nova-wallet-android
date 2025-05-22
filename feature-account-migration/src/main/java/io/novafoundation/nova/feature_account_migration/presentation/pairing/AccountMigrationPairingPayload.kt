package io.novafoundation.nova.feature_account_migration.presentation.pairing

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class AccountMigrationPairingPayload(
    val scheme: String
) : Parcelable
