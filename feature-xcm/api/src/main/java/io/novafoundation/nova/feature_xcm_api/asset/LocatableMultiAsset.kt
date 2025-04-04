package io.novafoundation.nova.feature_xcm_api.asset

import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.versions.VersionedXcm

class LocatableMultiAsset(

    val location: RelativeMultiLocation,

    val assetId: MultiAssetId
)

typealias VersionedLocatableMultiAsset = VersionedXcm<LocatableMultiAsset>
