package sandbox.serialization

import org.junit.jupiter.api.Test
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.reflect.KClass
import kotlin.test.assertEquals

object RealSerialization {
    @Serializable
    data class D1(val x: Int, val y: Float){}

    @Test
    fun convertJson() {
        val d = D1(13, 16.23f)
        val json = Json(JsonConfiguration.Stable)
        println(json.stringify(D1.serializer(),d))
    }

    @Test
    fun convertProtobuff() {
        val d = D1(16, 16.001f)
        val pb = ProtoBuf()
        val ser = pb.dump(D1.serializer(), d)
        val d2 = pb.load(D1.serializer(),ser)
        assertEquals(16, d2.x)
        assertEquals(16.001f, d2.y)
    }
}

object DynamicScoped {
    @Serializable
    data class D1(val x: Int, val y: Float){}

    interface ILoggableType<T : Any> {
        val serializer: KSerializer<T>
        val type : KClass<T>
    }

    class LoggableType<T: Any>(
            override val serializer: KSerializer<T>,
            override val type : KClass<T> )  : ILoggableType<T>

    interface ILoggable<T : Any> {
        val type: ILoggableType<T>
        val id: Int
    }

    @Serializable
    data class ThisLoggable(val x: Int)

    class TLL: ILoggable<ThisLoggable> {
        override val id = 1
        override val type = LoggableType(
                ThisLoggable.serializer(),
                ThisLoggable::class)
    }

    object SerializerFactory {
        val map = mutableMapOf<Int, ILoggable<*>>()
        fun load(thing : ILoggable<*>){
            map[thing.id ] = thing
        }
    }

    @Test
    fun test() {
        val d = D1(0, 0f)
        val dtype = d::class
        println(dtype.objectInstance)
    }

    @Test
    fun dynamicTest() {
        val orig = ThisLoggable(10)
        val serialized = SerializerFactory.map[1]!!.type.serializer
    }
}