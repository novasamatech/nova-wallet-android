package io.novafoundation.nova.feature_ahm_impl.presentation.migrationDetails

import io.novafoundation.nova.common.mixin.hints.HintModel

class ConfigModel(
    val title: String,
    val minimalBalance: String,
    val lowerFee: String,
    val tokens: String,
    val unifiedAccess: String,
    val anyTokenFee: String,
    val hints: List<HintModel>
)
