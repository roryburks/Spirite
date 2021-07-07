package spirite.core.file.save

import rb.file.IWriteStream
import rb.vectrix.mathUtil.i
import spirite.core.file.SifConstants
import spirite.core.file.contracts.*

object SifImgdWriter {
    fun write(out: IWriteStream, data: SifImgdChunk) {
        for (medium in data.mediums) {
            out.writeInt(medium.id)

            when( medium.data) {
                is SifImgdMed_Plain -> {
                    out.writeByte(SifConstants.MEDIUM_PLAIN)
                    out.writeInt(medium.data.rawImgData.size)
                    out.write(medium.data.rawImgData)
                }
                is SifImgdMed_Dynamic -> {
                    out.writeByte(SifConstants.MEDIUM_DYNAMIC)
                    out.writeShort(medium.data.offsetX.i)
                    out.writeShort(medium.data.offsetY.i)
                    out.writeInt(medium.data.rawImgData?.size ?: 0)
                    if( medium.data.rawImgData != null)
                        out.write(medium.data.rawImgData)
                }
                is SifImgdMed_Maglev -> {
                    out.writeByte(SifConstants.MEDIUM_MAGLEV)

                    val things = medium.data.things.take(65535)
                    out.writeShort(things.size)
                    for (thing in things) {
                        when( thing ){
                            is SifImgdMagThing_Stroke->{
                                out.writeByte(SifConstants.MAGLEV_THING_STROKE)
                                out.writeInt(thing.color)
                                out.writeByte(thing.method.i)
                                out.writeFloat(thing.width)
                                out.writeByte(thing.drawMode.i)
                                out.writeInt(thing.xs.size)
                                out.writeFloatArray(thing.xs)
                                out.writeFloatArray(thing.ys)
                                out.writeFloatArray(thing.ws)
                            }
                            is SifImgdMagThing_Fill -> {
                                out.writeByte(SifConstants.MAGLEV_THING_FILL)
                                out.writeInt(thing.color)
                                out.writeByte(thing.medhod.i)

                                val refPoints = thing.refPoints.take(65535)
                                out.writeShort(refPoints.size)
                                for (refPoint in refPoints) {
                                    out.writeInt(refPoint.strokeRef)
                                    out.writeInt(refPoint.startPoint)
                                    out.writeInt(refPoint.endPoint)
                                }
                            }
                        }
                    }
                    out.writeInt(medium.data.rawImgData.size)
                    out.writeShort(medium.data.offsetX.i)
                    out.writeShort(medium.data.offsetY.i)
                    out.write(medium.data.rawImgData)
                }
                is SifImgdMed_Prismatic -> {}
            }

        }

    }
}