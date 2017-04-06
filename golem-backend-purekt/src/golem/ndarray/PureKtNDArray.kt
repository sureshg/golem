package golem.ndarray

open class PureKtNDArray<T>(vararg val strides: Int, init: (IntArray)->T): NDArray<T> {
    @Suppress("UNCHECKED_CAST")
    private val storage: Array<T> = Array(strides.reduce{a,b->a*b}, 
                                          {init(linearToNIdx(it, strides)) as Any}) as Array<T>
    
    override fun get(vararg indices: Int): T = storage[findIdx(indices)]
    override fun get(vararg indices: IntRange): NDArray<T> {
        return PureKtNDArray<T>(strides=*indices
                .map { it.last - it.first }
                .toIntArray()) { newIdxs ->
            val offsets = indices.map { it.first }
            val oldIdxs = newIdxs.zip(offsets).map { it.first + it.second }
            this.get(*oldIdxs.toIntArray())
        }
        
    }
    override operator fun set(vararg indices: Int, value: T) {
        storage[findIdx(indices)] = value
    }
    override fun set(vararg indices: IntRange, value: NDArray<T>) {
        value.mapArrIndexed { linear, _ ->
            val thisIdx = linearToNIdx(linear, value.shape().toIntArray())
                    .zip(indices.map { it.first() })
                    .map { it.first + it.second }
                    .toIntArray()
            this.get(*thisIdx)
        }
    }
    override fun shape(): List<Int> = strides.toList()
    override fun copy(): NDArray<T> = PureKtNDArray<T>(*strides, init={this.get(*it)})
    override fun getBaseArray(): Any = storage

    private fun findIdx(indices: IntArray): Int {
        var finalIdx = 0
        var cumStride = 1
        indices.reversed().zip(strides.reversed()).forEach { (idx, stride) ->
            cumStride*=stride
            finalIdx += idx*cumStride
        }
        return finalIdx
    }
    private fun linearToNIdx(linear:Int,
                             strides: IntArray): IntArray = TODO()
}
