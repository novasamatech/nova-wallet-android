package io.novafoundation.nova.feature_deep_linking.di

import io.novafoundation.nova.feature_deep_linking.presentation.handling.branchIo.BranchIoLinkConverter
import io.novafoundation.nova.feature_deep_linking.presentation.handling.common.DeepLinkingPreferences

interface DeepLinkingFeatureApi {
    val deepLinkingPreferences: DeepLinkingPreferences

    val branchIoLinkConverter: BranchIoLinkConverter
}
