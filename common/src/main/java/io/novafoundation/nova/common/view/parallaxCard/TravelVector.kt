package io.novafoundation.nova.common.view.parallaxCard

class TravelVector(var x: Float, var y: Float) {

    fun set(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    fun coerceIn(min: TravelVector, max: TravelVector): TravelVector {
        return TravelVector(x.coerceIn(min.x, max.x), y.coerceIn(min.y, max.y))
    }

    operator fun plus(other: TravelVector): TravelVector {
        return TravelVector(x + other.x, y + other.y)
    }

    operator fun minus(other: TravelVector): TravelVector {
        return TravelVector(x - other.x, y - other.y)
    }

    operator fun div(other: TravelVector): TravelVector {
        return TravelVector(x / other.x, y / other.y)
    }

    operator fun times(other: Float): TravelVector {
        return TravelVector(x * other, y * other)
    }

    operator fun times(other: TravelVector): TravelVector {
        return TravelVector(x * other.x, y * other.y)
    }

    operator fun unaryMinus(): TravelVector {
        return TravelVector(-x, -y)
    }
}

fun TravelVector.isZero(): Boolean {
    return x == 0f && y == 0f
}
