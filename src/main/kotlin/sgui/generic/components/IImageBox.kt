package sgui.generic.components

import rb.glow.IImage

interface IImageBox : IComponent {
    var stretch: Boolean
    var checkeredBackground: Boolean

    fun setImage( img: IImage?)
}

