package io.novafoundation.nova.feature_account_api.presenatation.chain

data class ChainUi(
    val id: String,
    val name: String,
    val gradient: GradientUi,
    val icon: String
)

@Suppress("ArrayInDataClass", "EqualsOrHashCode")
data class GradientUi(
    val angle: Int,
    val colors: IntArray,
    val positions: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        return other is GradientUi && angle == other.angle &&
            colors.contentEquals(other.colors) &&
            positions.contentEquals(other.positions)
    }
}
