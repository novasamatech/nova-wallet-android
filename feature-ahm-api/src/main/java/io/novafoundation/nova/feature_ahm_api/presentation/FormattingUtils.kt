package io.novafoundation.nova.feature_ahm_api.presentation

import java.text.SimpleDateFormat
import java.util.Locale

fun getChainMigrationDateFormat() = SimpleDateFormat("d MMMM, yyyy", Locale.getDefault())
