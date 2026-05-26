package io.novafoundation.nova.common.utils.blur

import android.graphics.Bitmap

object StackBlur {

    @JvmStatic
    fun blur(src: Bitmap, radius: Int): Bitmap {
        val r = radius.coerceIn(1, 254)

        val bitmap = src.copy(Bitmap.Config.ARGB_8888, true)
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return bitmap

        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = r + r + 1

        val aArr = IntArray(wh)
        val rArr = IntArray(wh)
        val gArr = IntArray(wh)
        val bArr = IntArray(wh)
        var asum: Int
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(maxOf(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }

        yw = 0
        yi = 0

        val stack = Array(div) { IntArray(4) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = r + 1
        var aoutsum: Int
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var ainsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        y = 0
        while (y < h) {
            asum = 0; rsum = 0; gsum = 0; bsum = 0
            aoutsum = 0; routsum = 0; goutsum = 0; boutsum = 0
            ainsum = 0; rinsum = 0; ginsum = 0; binsum = 0
            i = -r
            while (i <= r) {
                p = pix[yi + minOf(wm, maxOf(i, 0))]
                sir = stack[i + r]
                sir[0] = (p ushr 24) and 0xff
                sir[1] = (p shr 16) and 0xff
                sir[2] = (p shr 8) and 0xff
                sir[3] = p and 0xff
                rbs = r1 - kotlin.math.abs(i)
                asum += sir[0] * rbs
                rsum += sir[1] * rbs
                gsum += sir[2] * rbs
                bsum += sir[3] * rbs
                if (i > 0) {
                    ainsum += sir[0]; rinsum += sir[1]; ginsum += sir[2]; binsum += sir[3]
                } else {
                    aoutsum += sir[0]; routsum += sir[1]; goutsum += sir[2]; boutsum += sir[3]
                }
                i++
            }
            stackpointer = r

            x = 0
            while (x < w) {
                aArr[yi] = dv[asum]
                rArr[yi] = dv[rsum]
                gArr[yi] = dv[gsum]
                bArr[yi] = dv[bsum]

                asum -= aoutsum; rsum -= routsum; gsum -= goutsum; bsum -= boutsum

                stackstart = stackpointer - r + div
                sir = stack[stackstart % div]
                aoutsum -= sir[0]; routsum -= sir[1]; goutsum -= sir[2]; boutsum -= sir[3]

                if (y == 0) vmin[x] = minOf(x + r + 1, wm)
                p = pix[yw + vmin[x]]
                sir[0] = (p ushr 24) and 0xff
                sir[1] = (p shr 16) and 0xff
                sir[2] = (p shr 8) and 0xff
                sir[3] = p and 0xff
                ainsum += sir[0]; rinsum += sir[1]; ginsum += sir[2]; binsum += sir[3]
                asum += ainsum; rsum += rinsum; gsum += ginsum; bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                aoutsum += sir[0]; routsum += sir[1]; goutsum += sir[2]; boutsum += sir[3]
                ainsum -= sir[0]; rinsum -= sir[1]; ginsum -= sir[2]; binsum -= sir[3]

                yi++
                x++
            }
            yw += w
            y++
        }

        x = 0
        while (x < w) {
            asum = 0; rsum = 0; gsum = 0; bsum = 0
            aoutsum = 0; routsum = 0; goutsum = 0; boutsum = 0
            ainsum = 0; rinsum = 0; ginsum = 0; binsum = 0
            yp = -r * w
            i = -r
            while (i <= r) {
                yi = maxOf(0, yp) + x
                sir = stack[i + r]
                sir[0] = aArr[yi]
                sir[1] = rArr[yi]
                sir[2] = gArr[yi]
                sir[3] = bArr[yi]
                rbs = r1 - kotlin.math.abs(i)
                asum += aArr[yi] * rbs
                rsum += rArr[yi] * rbs
                gsum += gArr[yi] * rbs
                bsum += bArr[yi] * rbs
                if (i > 0) {
                    ainsum += sir[0]; rinsum += sir[1]; ginsum += sir[2]; binsum += sir[3]
                } else {
                    aoutsum += sir[0]; routsum += sir[1]; goutsum += sir[2]; boutsum += sir[3]
                }
                if (i < hm) yp += w
                i++
            }
            yi = x
            stackpointer = r
            y = 0
            while (y < h) {
                pix[yi] = (dv[asum] shl 24) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                asum -= aoutsum; rsum -= routsum; gsum -= goutsum; bsum -= boutsum
                stackstart = stackpointer - r + div
                sir = stack[stackstart % div]
                aoutsum -= sir[0]; routsum -= sir[1]; goutsum -= sir[2]; boutsum -= sir[3]
                if (x == 0) vmin[y] = minOf(y + r1, hm) * w
                p = x + vmin[y]
                sir[0] = aArr[p]; sir[1] = rArr[p]; sir[2] = gArr[p]; sir[3] = bArr[p]
                ainsum += sir[0]; rinsum += sir[1]; ginsum += sir[2]; binsum += sir[3]
                asum += ainsum; rsum += rinsum; gsum += ginsum; bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                aoutsum += sir[0]; routsum += sir[1]; goutsum += sir[2]; boutsum += sir[3]
                ainsum -= sir[0]; rinsum -= sir[1]; ginsum -= sir[2]; binsum -= sir[3]
                yi += w
                y++
            }
            x++
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }
}
