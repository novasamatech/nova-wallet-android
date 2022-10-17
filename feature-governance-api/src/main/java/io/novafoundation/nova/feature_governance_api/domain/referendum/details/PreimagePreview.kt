package io.novafoundation.nova.feature_governance_api.domain.referendum.details

sealed class PreimagePreview {

    object TooLong : PreimagePreview()

    class Display(val value: String) : PreimagePreview()
}
