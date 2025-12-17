package io.novafoundation.nova.feature_ahm_impl.presentation.migrationDetails

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class ChainMigrationDetailsPayload(val chainId: String) : Parcelable
