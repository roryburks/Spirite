package sjunit

import spirite.base.graphics.IImage

fun verifyRawImagesAreEqual(raw1: IImage, raw2: IImage) {
    assert(raw1.width == raw2.width)
    assert(raw1.height == raw2.height)

    for (x in 0 until raw1.width)
        for (y in 0 until raw1.height)
            assert(raw1.getRGB(x, y) == raw2.getRGB(x, y))
}