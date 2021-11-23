package io.codeherb.mycompose

import android.graphics.Color
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("io.codeherb.mycompose", appContext.packageName)
    }

    @Test
    fun colorTest() {
        val int = Color.parseColor("#C9132A")
        val fore = Color.parseColor("#80FFFFFF")
        println("color = ${overlay(int, fore)}, base alpha = ${int.alpha}")
        println("color Hex = ${"%x".format(overlay(int, fore))}")
    }

    fun overlay(base: Int, fore: Int) : Int {
        val r = base.red
        val g = base.green
        val b = base.blue
        val blend: (Int, Int) -> Int = if (getBrightness(r, g, b) < 128)
            { a, b -> (2 * a * b) / 255 }
        else
            { a, b -> 255 - ((2 * (255 - a) * (255 - b)) / 255) }

        return ((base.alpha and 0xFF) shl 24) or
                ((blend(r, fore.red) and 0xFF) shl 16) or
                ((blend(g, fore.green) and 0xFF) shl 8) or
                (blend(b, fore.blue) and 0xFF)
    }

    private fun getBrightness(r: Int, g: Int, b: Int): Int =
        listOf(r, r, b, g, g, g).sum() / 6
}