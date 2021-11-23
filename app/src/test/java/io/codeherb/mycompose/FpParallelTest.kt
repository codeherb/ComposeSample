package io.codeherb.mycompose

import android.graphics.Color
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import io.codeherb.mycompose.fp.*
import io.codeherb.mycompose.fp.CoPars.lazyUnit
import io.codeherb.mycompose.fp.CoPars.map2
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

class FpParallelTest {

    @Test
    fun testMergeSum() {
        assertEquals(10, sum(listOf(1,2,3,4)))
    }

    @Test
    fun testDivideAndConquerSum() {
        assertEquals(10, dncSum(listOf(1,2,3,4)))
    }

//    @Test
//    fun testParallelSum() {
//        assertEquals(10, parSum(listOf(1,2,3,4)).get)
//    }

    @Test
    fun coParTest() = runBlockingTest {
        val coSome = lazyUnit {
            delay(1000)
            "Da Dan!!"
        }
        val coSome2 = lazyUnit {
            delay(5000)
            "Hello"
        }
        println("@####")
        val result = map2(coSome, coSome2) { a, b ->
            a + b
        }
        val deferred = result(this)

        println("@## ${deferred.await()}")
    }

    @Test
    fun testMonoid() {

        val intsEndoMonoid = endoMonoid<Int>()
        val left = intsEndoMonoid.op({ it + 3 }, intsEndoMonoid.zero)
        val right = intsEndoMonoid.op(intsEndoMonoid.zero, { it + 3 })
        assertTrue(left(10) == right(10))

        val leftAssociate = intsEndoMonoid.op(intsEndoMonoid.op({ it + 8 }, { it - 3}), { it + 2 })
        val rightAssociate = intsEndoMonoid.op({ it + 8 }, intsEndoMonoid.op({ it - 3}, { it + 2 }))
        assertTrue(leftAssociate(10) == rightAssociate(10))
    }


}