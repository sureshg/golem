package golem.matrix.ejml

import golem.ceil
import golem.logb
import golem.pow
import golem.matrix.Matrix
import golem.matrix.ejml.backend.*
import org.ejml.simple.SimpleMatrix
import org.ejml.ops.*

class EJMLMatrix(var storage: SimpleMatrix) : Matrix<Double>
{

    override fun diag() = EJMLMatrix(storage.extractDiag())
    override fun cumsum() = storage.elementSum()
    override fun max() = CommonOps.elementMax(this.storage.matrix)
    override fun mean() = throw UnsupportedOperationException()
    override fun min() = CommonOps.elementMin(this.storage.matrix)
    override fun argMax() = throw UnsupportedOperationException()
    override fun argMean() = throw UnsupportedOperationException()
    override fun argMin() = throw UnsupportedOperationException()
    override fun norm() = this.storage.normF()
    override fun getDouble(i: Int, j: Int) = this.storage.get(i, j)
    override fun getDouble(i: Int) = this.storage.get(i)
    override fun setDouble(i: Int, v: Double) = this.storage.set(i, v)
    override fun setDouble(i: Int, j: Int, v: Double) = this.storage.set(i,j,v)

    override fun numRows() = this.storage.numRows()
    override fun numCols() = this.storage.numCols()
    override fun times(other: Matrix<Double>) = EJMLMatrix(this.storage.times(castOrBail(other).storage))
    override fun times(other: Double) = EJMLMatrix(this.storage.times(other))
    override fun elementTimes(other: Matrix<Double>) = EJMLMatrix(this.storage.elementMult(castOrBail(other).storage))
    override fun mod(other: Matrix<Double>) = EJMLMatrix(this.storage.mod(castOrBail(other).storage))
    override fun minus() = EJMLMatrix(this.storage.minus())
    override fun minus(other: Double) = EJMLMatrix(this.storage.minus(other))
    override fun minus(other: Matrix<Double>) = EJMLMatrix(this.storage.minus(castOrBail(other).storage))
    override fun div(other: Int) = EJMLMatrix(this.storage.div(other))
    override fun div(other: Double) = EJMLMatrix(this.storage.div(other))
    override fun transpose() = EJMLMatrix(this.storage.transpose())
    override fun set(i: Int, v: Double): Unit = if(this.storage.numRows()==1) this.storage.set(1,i,v) else this.storage.set(i,1,v)
    override fun set(i: Int, j: Int, v: Double) = this.storage.set(i,j,v)
    override fun get(i: Int, j: Int) = this.storage.get(i,j)
    override fun get(i: Int) = this.storage.get(i)
    override fun getRow(row: Int) = EJMLMatrix(SimpleMatrix(CommonOps.extractRow(this.storage.matrix, row, null)))
    override fun getCol(col: Int) = EJMLMatrix(SimpleMatrix(CommonOps.extractColumn(this.storage.matrix, col, null)))
    override fun plus(other: Matrix<Double>) = EJMLMatrix(this.storage.plus(castOrBail(other).storage))
    override fun plus(other: Double) = EJMLMatrix(this.storage.plus(other))
    override fun chol() = EJMLMatrix(SimpleMatrix(this.storage.chol().getT(null)))
    override fun inv() = EJMLMatrix(this.storage.inv())
    override fun det() = this.storage.determinant()
    override fun pinv()= EJMLMatrix(this.storage.pseudoInverse())
    override fun normf() = this.storage.normF()
    override fun elementSum() = this.storage.elementSum()
    override fun trace() = this.storage.trace()

    override fun setCol(index: Int, col: Matrix<Double>) {
        for (i in 0..col.numRows()-1)
            this[i,index] = col[i]
    }

    override fun setRow(index: Int, row: Matrix<Double>) {
        for (i in 0..row.numCols()-1)
            this[index, i] = row[i]
    }

    override fun getFactory() = golem.matrix.ejml.factory
    override val T: EJMLMatrix
        get() = this.transpose()

    override fun solve(A: Matrix<Double>, B: Matrix<Double>): EJMLMatrix {
        var out = this.getFactory().zeros(A.numCols(), 1)
        CommonOps.solve(castOrBail(A).storage.matrix, castOrBail(B).storage.matrix, out.storage.matrix)
        return out
    }

    override fun expm(): EJMLMatrix {
        var A = this
        var A_L1 = org.ejml.ops.NormOps.inducedP1(A.storage.matrix)
        var n_squarings = 0.0

        if (A_L1 < 1.495585217958292e-002) {
            val (U, V) = _pade3(A)
            return dispatchPade(U,V, n_squarings)
        }
        else if (A_L1 < 2.539398330063230e-001) {
            val (U, V) = _pade5(A)
            return dispatchPade(U,V, n_squarings)
        }
        else if (A_L1 < 9.504178996162932e-001) {
            val (U, V) = _pade7(A)
            return dispatchPade(U,V, n_squarings)
        }
        else if (A_L1 < 2.097847961257068e+000) {
            val (U, V) = _pade9(A)
            return dispatchPade(U,V, n_squarings)
        }
        else {

            var maxnorm = 5.371920351148152
            n_squarings = golem.max(0.0, ceil(logb(2, A_L1 / maxnorm))) //
            A = A / pow(2.0,n_squarings)
            val (U, V) = _pade13(A)
            return dispatchPade(U, V, n_squarings)
        }
    }
    private fun dispatchPade(U: EJMLMatrix, V: EJMLMatrix, n_squarings: Double): EJMLMatrix
    {
        var P = U+V
        var Q = -U+V
        //var R = solve(Q,P)
        var R = getFactory().zeros(Q.numCols(), P.numCols())
        for (i in 0..P.numCols()-1) {
            R.setCol(i, solve(Q, P.getCol(i)))

        }
        for (i in 0..n_squarings-1)
            R = R*R
        return R
    }

    private fun _pade3(A: EJMLMatrix): Pair<EJMLMatrix, EJMLMatrix>
    {
        var b = golem.mat[120, 60, 12, 1]
        var ident = getFactory().eye(A.numRows(), A.numCols())

        var A2 = A*A
        var U = A*(A2*b[3]+ident*b[1])
        var V = A2*b[2] + ident*b[0]

        return Pair(U,V)
    }
    private fun _pade5(A: EJMLMatrix): Pair<EJMLMatrix, EJMLMatrix>
    {
        var b = golem.mat[30240, 15120, 3360, 420, 30, 1]
        var ident = A.getFactory().eye(A.numRows(), A.numCols())
        var A2 = A*A
        var A4 = A2*A2
        var U = A*(A4*b[5]+A2*b[3]+ident*b[1])
        var V = A4*b[4]+A2*b[2]+ident*b[0]
        return Pair(U,V)

    }
    private fun _pade7(A: EJMLMatrix): Pair<EJMLMatrix, EJMLMatrix>
    {
        var b = golem.mat[17297280, 8648640, 1995840, 277200, 25200, 1512, 56, 1]
        var ident = A.getFactory().eye(A.numRows(), A.numCols())
        var A2 = A*A
        var A4 = A2*A2
        var A6 = A4*A2
        var U = A*(A6*b[7]+A4*b[5]+A2*b[3]+ident*b[1])
        var V = A6*b[6]+A4*b[4]+A2*b[2]+ident*b[0]
        return Pair(U,V)
    }
    private fun _pade9(A: EJMLMatrix): Pair<EJMLMatrix, EJMLMatrix>
    {
        var b = golem.mat[17643225600, 8821612800, 2075673600, 302702400, 30270240,
                2162160, 110880, 3960, 90, 1]
        var ident = getFactory().eye(A.numRows(), A.numCols())
        var A2 = A*A
        var A4 = A2*A2
        var A6 = A4*A2
        var A8 = A6*A2
        var U = A*(A8*b[9] + A6*b[7] + A4*b[5] + A2*b[3] + ident*b[1])
        var V = A8*b[8] + A6*b[6] + A4*b[4] + A2*b[2] + ident*b[0]
        return Pair(U,V)
    }
    private fun _pade13(A: EJMLMatrix): Pair<EJMLMatrix, EJMLMatrix>
    {
        var b = golem.mat[64764752532480000, 32382376266240000, 7771770303897600,
                1187353796428800, 129060195264000, 10559470521600, 670442572800,
                33522128640, 1323241920, 40840800, 960960, 16380, 182, 1]
        var ident = A.getFactory().eye(A.numRows(), A.numCols())

        var A2 = A*A
        var A4 = A2*A2
        var A6 = A4*A2
        var U = A*(A6*(A6*b[13] + A4*b[11] + A2*b[9]) + A6*b[7] + A4*b[5] + A2*b[3] + ident*b[1])
        var V = A6*(A6*b[12] + A4*b[10] + A2*b[8]) + A6*b[6] + A4*b[4] + A2*b[2] + ident*b[0]
        return Pair(U,V)
    }

    override fun LU(): Pair<EJMLMatrix, EJMLMatrix> {
        val decomp = this.storage.LU()
        return Pair(EJMLMatrix(SimpleMatrix(decomp.getLower(null))),
                    EJMLMatrix(SimpleMatrix(decomp.getUpper(null))))
    }

    override fun QR(): Pair<EJMLMatrix, EJMLMatrix> {
        val decomp = this.storage.QR()
        return Pair(EJMLMatrix(SimpleMatrix(decomp.getQ(null, false))),
                    EJMLMatrix(SimpleMatrix(decomp.getR(null, false))))
    }

    override fun repr() = this.storage.toString()

    // TODO: Fix this
    /**
     * Eventually we will support operations between matrices with different
     * backends. However, for now we'll exception out of it.
     *
     */
    private fun castOrBail(mat: Matrix<Double>): EJMLMatrix
    {
        when (mat) {
            is EJMLMatrix -> return mat
            else -> throw Exception("Operations between matrices with different backends not yet supported.")
        }

    }

    /* These methods are defined in order to support fast non-generic calls. However,
       since our type is Double we'll disable them here in case someone accidentally
       uses them.
     */
    override fun getInt(i: Int, j: Int): Int {
        throw UnsupportedOperationException("Implicit cast of Double matrix to Int disabled to prevent subtle bugs. " +
                "Please call getDouble and cast manually if this is intentional.")
    }
    override fun getFloat(i: Int, j: Int): Float {
        throw UnsupportedOperationException("Implicit cast of Double matrix to Float disabled to prevent subtle bugs. " +
                "Please call getDouble and cast manually if this is intentional.")
    }
    override fun getInt(i: Int): Int {
        throw UnsupportedOperationException("Implicit cast of Double matrix to Int disabled to prevent subtle bugs. " +
                "Please call getDouble and cast manually if this is intentional.")
    }

    override fun getFloat(i: Int): Float {
        throw UnsupportedOperationException("Implicit cast of Double matrix to Float disabled to prevent subtle bugs. " +
                "Please call getDouble and cast manually if this is intentional.")
    }

    override fun setInt(i: Int, v: Int) {
        throw UnsupportedOperationException("Implicit cast of Double matrix to Int disabled to prevent subtle bugs. " +
                "Please call getDouble and cast manually if this is intentional.")
    }

    override fun setFloat(i: Int, v: Float) {
        throw UnsupportedOperationException("Implicit cast of Double matrix to Float disabled to prevent subtle bugs. " +
                "Please call getDouble and cast manually if this is intentional.")
    }

    override fun setInt(i: Int, j: Int, v: Int) {
        throw UnsupportedOperationException("Implicit cast of Double matrix to Int disabled to prevent subtle bugs. " +
                "Please call getDouble and cast manually if this is intentional.")
    }
    override fun setFloat(i: Int, j: Int, v: Float) {
        throw UnsupportedOperationException("Implicit cast of Double matrix to Float disabled to prevent subtle bugs. " +
                "Please call getDouble and cast manually if this is intentional.")
    }

}

internal var factory: MatFactory = MatFactory()

