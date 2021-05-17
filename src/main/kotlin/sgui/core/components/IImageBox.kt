package sgui.core.components

import rb.glow.img.IImage
import sgui.components.IComponent

interface IImageBox : IComponent {
    var stretch: Boolean
    var checkeredBackground: Boolean

    fun setImage( img: IImage?)
}

