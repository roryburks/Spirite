package sjunit.spirite.graphics.gl

import spirite.base.resources.IScriptService

class TrivialScriptService : IScriptService {
    override fun loadScript(scriptName: String): String {
        if( scriptName.endsWith("frag"))
            return """#version 330

smooth in vec4 theColor;

out vec4 outputColor;

void main()
{
    outputColor = theColor;
}"""

        if( scriptName.endsWith("vert"))
            return """#version 330

layout (location = 0) in vec4 position;
layout (location = 1) in vec4 color;

flat out vec4 theColor;

void main()
{
    gl_Position = position;
    theColor = color;
}"""

        return ""
    }
}