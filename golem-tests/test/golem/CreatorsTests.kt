package golem

import golem.matrix.*
import golem.util.test.*
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

import golem.matrix.MatrixTypes.DoubleType as dbltype
import golem.matrix.MatrixTypes.FloatType as flttype
import golem.matrix.MatrixTypes.IntType as inttype

class CreatorsTests {
    @Test
    fun testZeros() {
        allBackends {
            val a = zeros(5, 1)
            for (i in 0..a.numRows() - 1)
                a[i, 0] = i + 1
            assertMatrixEquals(a, mat[1, 2, 3, 4, 5].T)
        }

    }

    @Test
    fun testCreate() {
        allBackends {
            val a = create(0..4)
            val b = mat[0, 1, 2, 3, 4]

            assertMatrixEquals(a, b)
            assertMatrixEquals(a.T, b.T)
        }
    }

    @Test
    fun testCopy() {
        allBackends {
            val a = mat[1, 2, 3 end 3, 4, 5]
            val b = a.copy()
            assertMatrixEquals(a,b)
            assertEquals(a.numRows(),b.numRows())
            assertEquals(a.numCols(),b.numCols())
        }
    }
    
    @Test
    fun testCreate2DShaped() {
        allBackends { 
            val a = arrayOf(1.0,2.0,3.0,4.0,5.0,6.0).toDoubleArray()
            val out = create(a, numRows = 2, numCols = 3)
            val expected = mat[1,2,3 end 
                               4,5,6]
            assertMatrixEquals(expected=expected,
                               actual=out)
        }
    }

    @Test
    fun testCreateJaggedArray() {
        allBackends {
            var a = arrayOf(doubleArrayOf(1.0, 2.0, 3.0),
                            doubleArrayOf(4.0, 5.0, 6.0))
            var out = create(a)
            assert(out[1, 0] == 4.0)
            assert(out[3] == 4.0)
            assert(out[1, 1] == 5.0)
            assert(out[1] == 2.0)

            a = arrayOf(doubleArrayOf(1.0, 2.0, 3.0))
            out = create(a)
            assert(out[0, 2] == 3.0)
            assertFails { out[2, 0] }

            a = arrayOf(doubleArrayOf(1.0),
                        doubleArrayOf(2.0),
                        doubleArrayOf(3.0))
            out = create(a)
            assert(out[2, 0] == 3.0)
            assertFails { out[0, 2] }
        }
    }

    @Test
    fun testOnes() {
        allBackends {
            val a = ones(3, 5)
            assertEquals(a[4], 1.0)
            assertEquals(a[2, 2], 1.0)
            assertMatrixEquals(zeros(3, 5).fill { i, j -> 1.0 }, a)
        }
    }

    @Test
    fun testEye() {
        allBackends {
            val a = eye(3)
            val expected = zeros(3, 3).mapMatIndexed { i, j, d -> if (i == j) 1.0 else 0.0 }
            assertMatrixEquals(expected, a)
        }
    }

    @Test
    fun testArange() {
        allBackends {
            val a = arange(1.0, 1.5, .1)
            val expected = mat[1.0, 1.1, 1.2, 1.3, 1.4]
            assertMatrixEquals(expected, a)
        }
    }

    @Test
    fun testRandn() {
        allBackends {
            val a = 2 * randn(1, 1000000)
            Assert.assertEquals(0.0, mean(a), .01)
            val aAgg = zeros(1, 1000000).mapMat { 2*randn(1)[0] }
            Assert.assertEquals(0.0, mean(aAgg), .01)

            val b = randn(3)
            val c = randn(3)
            assertFalse { (b-c).any { it == 0.0 } }
        }
    }
    @Test
    fun testRand() {
        allBackends {
            val a = 2 * rand(1, 1000000)
            Assert.assertEquals(1.0, mean(a), .01)
            val aAgg = zeros(1, 1000000).mapMat { 2*rand(1)[0] }
            Assert.assertEquals(1.0, mean(aAgg), .01)

            val b = rand(3)
            val c = rand(3)
            assertFalse { (b-c).any { it == 0.0 } }
        }
    }

    @Test
    fun testDTypeUsage() {
        val a: Matrix<Double> = zeros(3, 3, dtype=dbltype)
        assertFailsWith(RuntimeException::class,
                        "No default backends for golem matrix found. Please set golem.intFactory manually or put one on your classpath.") {
            val b: Matrix<Float> = zeros(3, 3, dtype=flttype)
            val c: Matrix<Int> = zeros(3, 3, dtype=inttype)
        }
    }
}
