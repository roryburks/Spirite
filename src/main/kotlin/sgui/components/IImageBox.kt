package sgui.components

import rb.glow.img.IImage

interface IImageBox : IComponent {
    var stretch: Boolean
    var checkeredBackground: Boolean

    fun setImage( img: IImage?)
}

