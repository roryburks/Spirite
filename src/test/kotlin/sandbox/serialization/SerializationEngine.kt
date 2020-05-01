package sandbox.serialization

import org.junit.jupiter.api.Test
import kotlin.reflect.full.memberProperties

object SerializationEngineTests {
    @Test
    fun test() {
        data class XYZ(val x: Int)
        SerializationEngine.serialize(XYZ(3))
    }

}

interface ISerializationEngine{
    fun <T:Any> serialize(t: T) : ByteArray
    fun <T> deserialize(byteArray: ByteArray) : T
}

object SerializationEngine2 : ISerializationEngine {


    override fun <T : Any> serialize(t: T): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T> deserialize(byteArray: ByteArray): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

object SerializationEngine : ISerializationEngine {
    override fun <T : Any> serialize(t: T): ByteArray {
        val x = t::class
        x.memberProperties
        return ByteArray(0)
    }

    override fun <T> deserialize(byteArray: ByteArray): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
