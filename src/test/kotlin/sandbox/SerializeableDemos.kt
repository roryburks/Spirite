package sandbox

import org.junit.jupiter.api.Test
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.assertEquals

object SerializeableDemos {
    @Serializable
    data class D1(val x: Int, val y: Float){}

    @Test
    fun convertJson() {
        val d = D1(13,16.23f)
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

}