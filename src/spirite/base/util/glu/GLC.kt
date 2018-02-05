package spirite.base.util.glu

/**
 * GLC is a namespace which includes all needed GL Constants whether the
 * local OpenGL version supports them or not.
 *
 * @author Guy Rory Burks
 */
object GLC {
    val GL_ACTIVE_TEXTURE = 0x84E0
    val GL_DEPTH_BUFFER_BIT = 0x00000100
    val GL_STENCIL_BUFFER_BIT = 0x00000400
    val GL_COLOR_BUFFER_BIT = 0x00004000
    val GL_FALSE = 0
    val GL_TRUE = 1
    val GL_POINTS = 0x0000
    val GL_LINES = 0x0001
    val GL_LINE_LOOP = 0x0002
    val GL_LINE_STRIP = 0x0003
    val GL_TRIANGLES = 0x0004
    val GL_TRIANGLE_STRIP = 0x0005
    val GL_TRIANGLE_FAN = 0x0006
    val GL_ZERO = 0
    val GL_ONE = 1
    val GL_SRC_COLOR = 0x0300
    val GL_ONE_MINUS_SRC_COLOR = 0x0301
    val GL_SRC_ALPHA = 0x0302
    val GL_ONE_MINUS_SRC_ALPHA = 0x0303
    val GL_DST_ALPHA = 0x0304
    val GL_ONE_MINUS_DST_ALPHA = 0x0305
    val GL_DST_COLOR = 0x0306
    val GL_ONE_MINUS_DST_COLOR = 0x0307
    val GL_SRC_ALPHA_SATURATE = 0x0308
    val GL_FUNC_ADD = 0x8006
    val GL_BLEND_EQUATION = 0x8009
    val GL_BLEND_EQUATION_RGB = 0x8009   /* same as BLEND_EQUATION */
    val GL_BLEND_EQUATION_ALPHA = 0x883D
    val GL_FUNC_SUBTRACT = 0x800A
    val GL_FUNC_REVERSE_SUBTRACT = 0x800B
    val GL_BLEND_DST_RGB = 0x80C8
    val GL_BLEND_SRC_RGB = 0x80C9
    val GL_BLEND_DST_ALPHA = 0x80CA
    val GL_BLEND_SRC_ALPHA = 0x80CB
    val GL_CONSTANT_COLOR = 0x8001
    val GL_ONE_MINUS_CONSTANT_COLOR = 0x8002
    val GL_CONSTANT_ALPHA = 0x8003
    val GL_ONE_MINUS_CONSTANT_ALPHA = 0x8004
    val GL_BLEND_COLOR = 0x8005
    val GL_ARRAY_BUFFER = 0x8892
    val GL_ELEMENT_ARRAY_BUFFER = 0x8893
    val GL_ARRAY_BUFFER_BINDING = 0x8894
    val GL_ELEMENT_ARRAY_BUFFER_BINDING = 0x8895
    val GL_STREAM_DRAW = 0x88E0
    val GL_STATIC_DRAW = 0x88E4
    val GL_DYNAMIC_DRAW = 0x88E8
    val GL_BUFFER_SIZE = 0x8764
    val GL_BUFFER_USAGE = 0x8765
    val GL_CURRENT_VERTEX_ATTRIB = 0x8626
    val GL_FRONT = 0x0404
    val GL_BACK = 0x0405
    val GL_FRONT_AND_BACK = 0x0408
    val GL_TEXTURE_2D = 0x0DE1
    val GL_CULL_FACE = 0x0B44
    val GL_BLEND = 0x0BE2
    val GL_DITHER = 0x0BD0
    val GL_STENCIL_TEST = 0x0B90
    val GL_DEPTH_TEST = 0x0B71
    val GL_SCISSOR_TEST = 0x0C11
    val GL_POLYGON_OFFSET_FILL = 0x8037
    val GL_SAMPLE_ALPHA_TO_COVERAGE = 0x809E
    val GL_SAMPLE_COVERAGE = 0x80A0
    val GL_NO_ERROR = 0
    val GL_INVALID_ENUM = 0x0500
    val GL_INVALID_VALUE = 0x0501
    val GL_INVALID_OPERATION = 0x0502
    val GL_OUT_OF_MEMORY = 0x0505
    val GL_CW = 0x0900
    val GL_CCW = 0x0901
    val GL_LINE_WIDTH = 0x0B21
    val GL_ALIASED_POINT_SIZE_RANGE = 0x846D
    val GL_ALIASED_LINE_WIDTH_RANGE = 0x846E
    val GL_CULL_FACE_MODE = 0x0B45
    val GL_FRONT_FACE = 0x0B46
    val GL_DEPTH_RANGE = 0x0B70
    val GL_DEPTH_WRITEMASK = 0x0B72
    val GL_DEPTH_CLEAR_VALUE = 0x0B73
    val GL_DEPTH_FUNC = 0x0B74
    val GL_STENCIL_CLEAR_VALUE = 0x0B91
    val GL_STENCIL_FUNC = 0x0B92
    val GL_STENCIL_FAIL = 0x0B94
    val GL_STENCIL_PASS_DEPTH_FAIL = 0x0B95
    val GL_STENCIL_PASS_DEPTH_PASS = 0x0B96
    val GL_STENCIL_REF = 0x0B97
    val GL_STENCIL_VALUE_MASK = 0x0B93
    val GL_STENCIL_WRITEMASK = 0x0B98
    val GL_STENCIL_BACK_FUNC = 0x8800
    val GL_STENCIL_BACK_FAIL = 0x8801
    val GL_STENCIL_BACK_PASS_DEPTH_FAIL = 0x8802
    val GL_STENCIL_BACK_PASS_DEPTH_PASS = 0x8803
    val GL_STENCIL_BACK_REF = 0x8CA3
    val GL_STENCIL_BACK_VALUE_MASK = 0x8CA4
    val GL_STENCIL_BACK_WRITEMASK = 0x8CA5
    val GL_VIEWPORT = 0x0BA2
    val GL_SCISSOR_BOX = 0x0C10
    val GL_COLOR_CLEAR_VALUE = 0x0C22
    val GL_COLOR_WRITEMASK = 0x0C23
    val GL_UNPACK_ALIGNMENT = 0x0CF5
    val GL_PACK_ALIGNMENT = 0x0D05
    val GL_MAX_TEXTURE_SIZE = 0x0D33
    val GL_MAX_VIEWPORT_DIMS = 0x0D3A
    val GL_SUBPIXEL_BITS = 0x0D50
    val GL_RED_BITS = 0x0D52
    val GL_GREEN_BITS = 0x0D53
    val GL_BLUE_BITS = 0x0D54
    val GL_ALPHA_BITS = 0x0D55
    val GL_DEPTH_BITS = 0x0D56
    val GL_STENCIL_BITS = 0x0D57
    val GL_POLYGON_OFFSET_UNITS = 0x2A00
    val GL_POLYGON_OFFSET_FACTOR = 0x8038
    val GL_TEXTURE_BINDING_2D = 0x8069
    val GL_SAMPLE_BUFFERS = 0x80A8
    val GL_SAMPLES = 0x80A9
    val GL_SAMPLE_COVERAGE_VALUE = 0x80AA
    val GL_SAMPLE_COVERAGE_INVERT = 0x80AB
    val GL_NUM_COMPRESSED_TEXTURE_FORMATS = 0x86A2
    val GL_COMPRESSED_TEXTURE_FORMATS = 0x86A3
    val GL_DONT_CARE = 0x1100
    val GL_FASTEST = 0x1101
    val GL_NICEST = 0x1102
    val GL_GENERATE_MIPMAP_HINT = 0x8192
    val GL_BYTE = 0x1400
    val GL_UNSIGNED_BYTE = 0x1401
    val GL_SHORT = 0x1402
    val GL_UNSIGNED_SHORT = 0x1403
    val GL_INT = 0x1404
    val GL_UNSIGNED_INT = 0x1405
    val GL_FLOAT = 0x1406
    val GL_FIXED = 0x140C
    val GL_DEPTH_COMPONENT = 0x1902
    val GL_ALPHA = 0x1906
    val GL_RGB = 0x1907
    val GL_RGBA = 0x1908
    val GL_LUMINANCE = 0x1909
    val GL_LUMINANCE_ALPHA = 0x190A
    val GL_UNSIGNED_SHORT_4_4_4_4 = 0x8033
    val GL_UNSIGNED_SHORT_5_5_5_1 = 0x8034
    val GL_UNSIGNED_SHORT_5_6_5 = 0x8363
    val GL_FRAGMENT_SHADER = 0x8B30
    val GL_VERTEX_SHADER = 0x8B31
    val GL_MAX_VERTEX_ATTRIBS = 0x8869
    val GL_MAX_VERTEX_UNIFORM_VECTORS = 0x8DFB
    val GL_MAX_VARYING_VECTORS = 0x8DFC
    val GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS = 0x8B4D
    val GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS = 0x8B4C
    val GL_MAX_TEXTURE_IMAGE_UNITS = 0x8872
    val GL_MAX_FRAGMENT_UNIFORM_VECTORS = 0x8DFD
    val GL_SHADER_TYPE = 0x8B4F
    val GL_DELETE_STATUS = 0x8B80
    val GL_LINK_STATUS = 0x8B82
    val GL_VALIDATE_STATUS = 0x8B83
    val GL_ATTACHED_SHADERS = 0x8B85
    val GL_ACTIVE_UNIFORMS = 0x8B86
    val GL_ACTIVE_UNIFORM_MAX_LENGTH = 0x8B87
    val GL_ACTIVE_ATTRIBUTES = 0x8B89
    val GL_ACTIVE_ATTRIBUTE_MAX_LENGTH = 0x8B8A
    val GL_SHADING_LANGUAGE_VERSION = 0x8B8C
    val GL_CURRENT_PROGRAM = 0x8B8D
    val GL_NEVER = 0x0200
    val GL_LESS = 0x0201
    val GL_EQUAL = 0x0202
    val GL_LEQUAL = 0x0203
    val GL_GREATER = 0x0204
    val GL_NOTEQUAL = 0x0205
    val GL_GEQUAL = 0x0206
    val GL_ALWAYS = 0x0207
    val GL_KEEP = 0x1E00
    val GL_REPLACE = 0x1E01
    val GL_INCR = 0x1E02
    val GL_DECR = 0x1E03
    val GL_INVERT = 0x150A
    val GL_INCR_WRAP = 0x8507
    val GL_DECR_WRAP = 0x8508
    val GL_VENDOR = 0x1F00
    val GL_RENDERER = 0x1F01
    val GL_VERSION = 0x1F02
    val GL_EXTENSIONS = 0x1F03
    val GL_NEAREST = 0x2600
    val GL_LINEAR = 0x2601
    val GL_NEAREST_MIPMAP_NEAREST = 0x2700
    val GL_LINEAR_MIPMAP_NEAREST = 0x2701
    val GL_NEAREST_MIPMAP_LINEAR = 0x2702
    val GL_LINEAR_MIPMAP_LINEAR = 0x2703
    val GL_TEXTURE_MAG_FILTER = 0x2800
    val GL_TEXTURE_MIN_FILTER = 0x2801
    val GL_TEXTURE_WRAP_S = 0x2802
    val GL_TEXTURE_WRAP_T = 0x2803
    val GL_TEXTURE = 0x1702
    val GL_TEXTURE_CUBE_MAP = 0x8513
    val GL_TEXTURE_BINDING_CUBE_MAP = 0x8514
    val GL_TEXTURE_CUBE_MAP_POSITIVE_X = 0x8515
    val GL_TEXTURE_CUBE_MAP_NEGATIVE_X = 0x8516
    val GL_TEXTURE_CUBE_MAP_POSITIVE_Y = 0x8517
    val GL_TEXTURE_CUBE_MAP_NEGATIVE_Y = 0x8518
    val GL_TEXTURE_CUBE_MAP_POSITIVE_Z = 0x8519
    val GL_TEXTURE_CUBE_MAP_NEGATIVE_Z = 0x851A
    val GL_MAX_CUBE_MAP_TEXTURE_SIZE = 0x851C
    val GL_TEXTURE0 = 0x84C0
    val GL_TEXTURE1 = 0x84C1
    val GL_TEXTURE2 = 0x84C2
    val GL_TEXTURE3 = 0x84C3
    val GL_TEXTURE4 = 0x84C4
    val GL_TEXTURE5 = 0x84C5
    val GL_TEXTURE6 = 0x84C6
    val GL_TEXTURE7 = 0x84C7
    val GL_TEXTURE8 = 0x84C8
    val GL_TEXTURE9 = 0x84C9
    val GL_TEXTURE10 = 0x84CA
    val GL_TEXTURE11 = 0x84CB
    val GL_TEXTURE12 = 0x84CC
    val GL_TEXTURE13 = 0x84CD
    val GL_TEXTURE14 = 0x84CE
    val GL_TEXTURE15 = 0x84CF
    val GL_TEXTURE16 = 0x84D0
    val GL_TEXTURE17 = 0x84D1
    val GL_TEXTURE18 = 0x84D2
    val GL_TEXTURE19 = 0x84D3
    val GL_TEXTURE20 = 0x84D4
    val GL_TEXTURE21 = 0x84D5
    val GL_TEXTURE22 = 0x84D6
    val GL_TEXTURE23 = 0x84D7
    val GL_TEXTURE24 = 0x84D8
    val GL_TEXTURE25 = 0x84D9
    val GL_TEXTURE26 = 0x84DA
    val GL_TEXTURE27 = 0x84DB
    val GL_TEXTURE28 = 0x84DC
    val GL_TEXTURE29 = 0x84DD
    val GL_TEXTURE30 = 0x84DE
    val GL_TEXTURE31 = 0x84DF
    val GL_REPEAT = 0x2901
    val GL_CLAMP_TO_EDGE = 0x812F
    val GL_MIRRORED_REPEAT = 0x8370
    val GL_FLOAT_VEC2 = 0x8B50
    val GL_FLOAT_VEC3 = 0x8B51
    val GL_FLOAT_VEC4 = 0x8B52
    val GL_INT_VEC2 = 0x8B53
    val GL_INT_VEC3 = 0x8B54
    val GL_INT_VEC4 = 0x8B55
    val GL_BOOL = 0x8B56
    val GL_BOOL_VEC2 = 0x8B57
    val GL_BOOL_VEC3 = 0x8B58
    val GL_BOOL_VEC4 = 0x8B59
    val GL_FLOAT_MAT2 = 0x8B5A
    val GL_FLOAT_MAT3 = 0x8B5B
    val GL_FLOAT_MAT4 = 0x8B5C
    val GL_SAMPLER_2D = 0x8B5E
    val GL_SAMPLER_CUBE = 0x8B60
    val GL_VERTEX_ATTRIB_ARRAY_ENABLED = 0x8622
    val GL_VERTEX_ATTRIB_ARRAY_SIZE = 0x8623
    val GL_VERTEX_ATTRIB_ARRAY_STRIDE = 0x8624
    val GL_VERTEX_ATTRIB_ARRAY_TYPE = 0x8625
    val GL_VERTEX_ATTRIB_ARRAY_NORMALIZED = 0x886A
    val GL_VERTEX_ATTRIB_ARRAY_POINTER = 0x8645
    val GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING = 0x889F
    val GL_IMPLEMENTATION_COLOR_READ_TYPE = 0x8B9A
    val GL_IMPLEMENTATION_COLOR_READ_FORMAT = 0x8B9B
    val GL_COMPILE_STATUS = 0x8B81
    val GL_INFO_LOG_LENGTH = 0x8B84
    val GL_SHADER_SOURCE_LENGTH = 0x8B88
    val GL_SHADER_COMPILER = 0x8DFA
    val GL_SHADER_BINARY_FORMATS = 0x8DF8
    val GL_NUM_SHADER_BINARY_FORMATS = 0x8DF9
    val GL_LOW_FLOAT = 0x8DF0
    val GL_MEDIUM_FLOAT = 0x8DF1
    val GL_HIGH_FLOAT = 0x8DF2
    val GL_LOW_INT = 0x8DF3
    val GL_MEDIUM_INT = 0x8DF4
    val GL_HIGH_INT = 0x8DF5
    val GL_FRAMEBUFFER = 0x8D40
    val GL_RENDERBUFFER = 0x8D41
    val GL_RGBA4 = 0x8056
    val GL_RGB5_A1 = 0x8057
    val GL_RGB565 = 0x8D62
    val GL_DEPTH_COMPONENT16 = 0x81A5
    // GL_STENCIL_INDEX does not appear in gl2.h or gl2ext.h, and there is no
    // token with value 0x1901.
    //
    @Deprecated("")
    val GL_STENCIL_INDEX = 0x1901
    val GL_STENCIL_INDEX8 = 0x8D48
    val GL_RENDERBUFFER_WIDTH = 0x8D42
    val GL_RENDERBUFFER_HEIGHT = 0x8D43
    val GL_RENDERBUFFER_INTERNAL_FORMAT = 0x8D44
    val GL_RENDERBUFFER_RED_SIZE = 0x8D50
    val GL_RENDERBUFFER_GREEN_SIZE = 0x8D51
    val GL_RENDERBUFFER_BLUE_SIZE = 0x8D52
    val GL_RENDERBUFFER_ALPHA_SIZE = 0x8D53
    val GL_RENDERBUFFER_DEPTH_SIZE = 0x8D54
    val GL_RENDERBUFFER_STENCIL_SIZE = 0x8D55
    val GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE = 0x8CD0
    val GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME = 0x8CD1
    val GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL = 0x8CD2
    val GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE = 0x8CD3
    val GL_COLOR_ATTACHMENT0 = 0x8CE0
    val GL_DEPTH_ATTACHMENT = 0x8D00
    val GL_STENCIL_ATTACHMENT = 0x8D20
    val GL_NONE = 0
    val GL_FRAMEBUFFER_COMPLETE = 0x8CD5
    val GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT = 0x8CD6
    val GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 0x8CD7
    val GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS = 0x8CD9
    val GL_FRAMEBUFFER_UNSUPPORTED = 0x8CDD
    val GL_FRAMEBUFFER_BINDING = 0x8CA6
    val GL_RENDERBUFFER_BINDING = 0x8CA7
    val GL_MAX_RENDERBUFFER_SIZE = 0x84E8
    val GL_INVALID_FRAMEBUFFER_OPERATION = 0x0506

    val GL_CONTEXT_FLAG_DEBUG_BIT = 0x00000002

    val GL_CONTEXT_FLAG_ROBUST_ACCESS_BIT = 0x00000004

    val GL_GEOMETRY_SHADER_BIT = 0x00000004
    val GL_TESS_CONTROL_SHADER_BIT = 0x00000008
    val GL_TESS_EVALUATION_SHADER_BIT = 0x00000010

    val GL_QUADS = 0x0007
    val GL_LINES_ADJACENCY = 0x000A
    val GL_LINE_STRIP_ADJACENCY = 0x000B
    val GL_TRIANGLES_ADJACENCY = 0x000C
    val GL_TRIANGLE_STRIP_ADJACENCY = 0x000D
    val GL_PATCHES = 0x000E
    val GL_STACK_OVERFLOW = 0x0503
    val GL_STACK_UNDERFLOW = 0x0504
    val GL_CONTEXT_LOST = 0x0507
    val GL_TEXTURE_BORDER_COLOR = 0x1004
    val GL_VERTEX_ARRAY = 0x8074
    val GL_CLAMP_TO_BORDER = 0x812D
    val GL_CONTEXT_FLAGS = 0x821E
    val GL_PRIMITIVE_RESTART_FOR_PATCHES_SUPPORTED = 0x8221
    val GL_DEBUG_OUTPUT_SYNCHRONOUS = 0x8242
    val GL_DEBUG_NEXT_LOGGED_MESSAGE_LENGTH = 0x8243
    val GL_DEBUG_CALLBACK_FUNCTION = 0x8244
    val GL_DEBUG_CALLBACK_USER_PARAM = 0x8245
    val GL_DEBUG_SOURCE_API = 0x8246
    val GL_DEBUG_SOURCE_WINDOW_SYSTEM = 0x8247
    val GL_DEBUG_SOURCE_SHADER_COMPILER = 0x8248
    val GL_DEBUG_SOURCE_THIRD_PARTY = 0x8249
    val GL_DEBUG_SOURCE_APPLICATION = 0x824A
    val GL_DEBUG_SOURCE_OTHER = 0x824B
    val GL_DEBUG_TYPE_ERROR = 0x824C
    val GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR = 0x824D
    val GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR = 0x824E
    val GL_DEBUG_TYPE_PORTABILITY = 0x824F
    val GL_DEBUG_TYPE_PERFORMANCE = 0x8250
    val GL_DEBUG_TYPE_OTHER = 0x8251
    val GL_LOSE_CONTEXT_ON_RESET = 0x8252
    val GL_GUILTY_CONTEXT_RESET = 0x8253
    val GL_INNOCENT_CONTEXT_RESET = 0x8254
    val GL_UNKNOWN_CONTEXT_RESET = 0x8255
    val GL_RESET_NOTIFICATION_STRATEGY = 0x8256
    val GL_LAYER_PROVOKING_VERTEX = 0x825E
    val GL_UNDEFINED_VERTEX = 0x8260
    val GL_NO_RESET_NOTIFICATION = 0x8261
    val GL_DEBUG_TYPE_MARKER = 0x8268
    val GL_DEBUG_TYPE_PUSH_GROUP = 0x8269
    val GL_DEBUG_TYPE_POP_GROUP = 0x826A
    val GL_DEBUG_SEVERITY_NOTIFICATION = 0x826B
    val GL_MAX_DEBUG_GROUP_STACK_DEPTH = 0x826C
    val GL_DEBUG_GROUP_STACK_DEPTH = 0x826D
    val GL_BUFFER = 0x82E0
    val GL_SHADER = 0x82E1
    val GL_PROGRAM = 0x82E2
    val GL_QUERY = 0x82E3
    val GL_PROGRAM_PIPELINE = 0x82E4
    val GL_SAMPLER = 0x82E6
    val GL_MAX_LABEL_LENGTH = 0x82E8
    val GL_MAX_TESS_CONTROL_INPUT_COMPONENTS = 0x886C
    val GL_MAX_TESS_EVALUATION_INPUT_COMPONENTS = 0x886D
    val GL_GEOMETRY_SHADER_INVOCATIONS = 0x887F
    val GL_GEOMETRY_VERTICES_OUT = 0x8916
    val GL_GEOMETRY_INPUT_TYPE = 0x8917
    val GL_GEOMETRY_OUTPUT_TYPE = 0x8918
    val GL_MAX_GEOMETRY_UNIFORM_BLOCKS = 0x8A2C
    val GL_MAX_COMBINED_GEOMETRY_UNIFORM_COMPONENTS = 0x8A32
    val GL_MAX_GEOMETRY_TEXTURE_IMAGE_UNITS = 0x8C29
    val GL_TEXTURE_BUFFER = 0x8C2A
    val GL_TEXTURE_BUFFER_BINDING = 0x8C2A
    val GL_MAX_TEXTURE_BUFFER_SIZE = 0x8C2B
    val GL_TEXTURE_BINDING_BUFFER = 0x8C2C
    val GL_TEXTURE_BUFFER_DATA_STORE_BINDING = 0x8C2D
    val GL_SAMPLE_SHADING = 0x8C36
    val GL_MIN_SAMPLE_SHADING_VALUE = 0x8C37
    val GL_PRIMITIVES_GENERATED = 0x8C87
    val GL_FRAMEBUFFER_ATTACHMENT_LAYERED = 0x8DA7
    val GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS = 0x8DA8
    val GL_SAMPLER_BUFFER = 0x8DC2
    val GL_INT_SAMPLER_BUFFER = 0x8DD0
    val GL_UNSIGNED_INT_SAMPLER_BUFFER = 0x8DD8
    val GL_GEOMETRY_SHADER = 0x8DD9
    val GL_MAX_GEOMETRY_UNIFORM_COMPONENTS = 0x8DDF
    val GL_MAX_GEOMETRY_OUTPUT_VERTICES = 0x8DE0
    val GL_MAX_GEOMETRY_TOTAL_OUTPUT_COMPONENTS = 0x8DE1
    val GL_MAX_COMBINED_TESS_CONTROL_UNIFORM_COMPONENTS = 0x8E1E
    val GL_MAX_COMBINED_TESS_EVALUATION_UNIFORM_COMPONENTS = 0x8E1F
    val GL_FIRST_VERTEX_CONVENTION = 0x8E4D
    val GL_LAST_VERTEX_CONVENTION = 0x8E4E
    val GL_MAX_GEOMETRY_SHADER_INVOCATIONS = 0x8E5A
    val GL_MIN_FRAGMENT_INTERPOLATION_OFFSET = 0x8E5B
    val GL_MAX_FRAGMENT_INTERPOLATION_OFFSET = 0x8E5C
    val GL_FRAGMENT_INTERPOLATION_OFFSET_BITS = 0x8E5D
    val GL_PATCH_VERTICES = 0x8E72
    val GL_TESS_CONTROL_OUTPUT_VERTICES = 0x8E75
    val GL_TESS_GEN_MODE = 0x8E76
    val GL_TESS_GEN_SPACING = 0x8E77
    val GL_TESS_GEN_VERTEX_ORDER = 0x8E78
    val GL_TESS_GEN_POINT_MODE = 0x8E79
    val GL_ISOLINES = 0x8E7A
    val GL_FRACTIONAL_ODD = 0x8E7B
    val GL_FRACTIONAL_EVEN = 0x8E7C
    val GL_MAX_PATCH_VERTICES = 0x8E7D
    val GL_MAX_TESS_GEN_LEVEL = 0x8E7E
    val GL_MAX_TESS_CONTROL_UNIFORM_COMPONENTS = 0x8E7F
    val GL_MAX_TESS_EVALUATION_UNIFORM_COMPONENTS = 0x8E80
    val GL_MAX_TESS_CONTROL_TEXTURE_IMAGE_UNITS = 0x8E81
    val GL_MAX_TESS_EVALUATION_TEXTURE_IMAGE_UNITS = 0x8E82
    val GL_MAX_TESS_CONTROL_OUTPUT_COMPONENTS = 0x8E83
    val GL_MAX_TESS_PATCH_COMPONENTS = 0x8E84
    val GL_MAX_TESS_CONTROL_TOTAL_OUTPUT_COMPONENTS = 0x8E85
    val GL_MAX_TESS_EVALUATION_OUTPUT_COMPONENTS = 0x8E86
    val GL_TESS_EVALUATION_SHADER = 0x8E87
    val GL_TESS_CONTROL_SHADER = 0x8E88
    val GL_MAX_TESS_CONTROL_UNIFORM_BLOCKS = 0x8E89
    val GL_MAX_TESS_EVALUATION_UNIFORM_BLOCKS = 0x8E8A
    val GL_TEXTURE_CUBE_MAP_ARRAY = 0x9009
    val GL_TEXTURE_BINDING_CUBE_MAP_ARRAY = 0x900A
    val GL_SAMPLER_CUBE_MAP_ARRAY = 0x900C
    val GL_SAMPLER_CUBE_MAP_ARRAY_SHADOW = 0x900D
    val GL_INT_SAMPLER_CUBE_MAP_ARRAY = 0x900E
    val GL_UNSIGNED_INT_SAMPLER_CUBE_MAP_ARRAY = 0x900F
    val GL_IMAGE_BUFFER = 0x9051
    val GL_IMAGE_CUBE_MAP_ARRAY = 0x9054
    val GL_INT_IMAGE_BUFFER = 0x905C
    val GL_INT_IMAGE_CUBE_MAP_ARRAY = 0x905F
    val GL_UNSIGNED_INT_IMAGE_BUFFER = 0x9067
    val GL_UNSIGNED_INT_IMAGE_CUBE_MAP_ARRAY = 0x906A
    val GL_MAX_TESS_CONTROL_IMAGE_UNIFORMS = 0x90CB
    val GL_MAX_TESS_EVALUATION_IMAGE_UNIFORMS = 0x90CC
    val GL_MAX_GEOMETRY_IMAGE_UNIFORMS = 0x90CD
    val GL_MAX_GEOMETRY_SHADER_STORAGE_BLOCKS = 0x90D7
    val GL_MAX_TESS_CONTROL_SHADER_STORAGE_BLOCKS = 0x90D8
    val GL_MAX_TESS_EVALUATION_SHADER_STORAGE_BLOCKS = 0x90D9
    val GL_TEXTURE_2D_MULTISAMPLE_ARRAY = 0x9102
    val GL_TEXTURE_BINDING_2D_MULTISAMPLE_ARRAY = 0x9105
    val GL_SAMPLER_2D_MULTISAMPLE_ARRAY = 0x910B
    val GL_INT_SAMPLER_2D_MULTISAMPLE_ARRAY = 0x910C
    val GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY = 0x910D
    val GL_MAX_GEOMETRY_INPUT_COMPONENTS = 0x9123
    val GL_MAX_GEOMETRY_OUTPUT_COMPONENTS = 0x9124
    val GL_MAX_DEBUG_MESSAGE_LENGTH = 0x9143
    val GL_MAX_DEBUG_LOGGED_MESSAGES = 0x9144
    val GL_DEBUG_LOGGED_MESSAGES = 0x9145
    val GL_DEBUG_SEVERITY_HIGH = 0x9146
    val GL_DEBUG_SEVERITY_MEDIUM = 0x9147
    val GL_DEBUG_SEVERITY_LOW = 0x9148
    val GL_TEXTURE_BUFFER_OFFSET = 0x919D
    val GL_TEXTURE_BUFFER_SIZE = 0x919E
    val GL_TEXTURE_BUFFER_OFFSET_ALIGNMENT = 0x919F
    val GL_MULTIPLY = 0x9294
    val GL_SCREEN = 0x9295
    val GL_OVERLAY = 0x9296
    val GL_DARKEN = 0x9297
    val GL_LIGHTEN = 0x9298
    val GL_COLORDODGE = 0x9299
    val GL_COLORBURN = 0x929A
    val GL_HARDLIGHT = 0x929B
    val GL_SOFTLIGHT = 0x929C
    val GL_DIFFERENCE = 0x929E
    val GL_EXCLUSION = 0x92A0
    val GL_HSL_HUE = 0x92AD
    val GL_HSL_SATURATION = 0x92AE
    val GL_HSL_COLOR = 0x92AF
    val GL_HSL_LUMINOSITY = 0x92B0
    val GL_PRIMITIVE_BOUNDING_BOX = 0x92BE
    val GL_MAX_TESS_CONTROL_ATOMIC_COUNTER_BUFFERS = 0x92CD
    val GL_MAX_TESS_EVALUATION_ATOMIC_COUNTER_BUFFERS = 0x92CE
    val GL_MAX_GEOMETRY_ATOMIC_COUNTER_BUFFERS = 0x92CF
    val GL_MAX_TESS_CONTROL_ATOMIC_COUNTERS = 0x92D3
    val GL_MAX_TESS_EVALUATION_ATOMIC_COUNTERS = 0x92D4
    val GL_MAX_GEOMETRY_ATOMIC_COUNTERS = 0x92D5
    val GL_DEBUG_OUTPUT = 0x92E0
    val GL_IS_PER_PATCH = 0x92E7
    val GL_REFERENCED_BY_TESS_CONTROL_SHADER = 0x9307
    val GL_REFERENCED_BY_TESS_EVALUATION_SHADER = 0x9308
    val GL_REFERENCED_BY_GEOMETRY_SHADER = 0x9309
    val GL_FRAMEBUFFER_DEFAULT_LAYERS = 0x9312
    val GL_MAX_FRAMEBUFFER_LAYERS = 0x9317
    val GL_MULTISAMPLE_LINE_WIDTH_RANGE = 0x9381
    val GL_MULTISAMPLE_LINE_WIDTH_GRANULARITY = 0x9382
    val GL_COMPRESSED_RGBA_ASTC_4x4 = 0x93B0
    val GL_COMPRESSED_RGBA_ASTC_5x4 = 0x93B1
    val GL_COMPRESSED_RGBA_ASTC_5x5 = 0x93B2
    val GL_COMPRESSED_RGBA_ASTC_6x5 = 0x93B3
    val GL_COMPRESSED_RGBA_ASTC_6x6 = 0x93B4
    val GL_COMPRESSED_RGBA_ASTC_8x5 = 0x93B5
    val GL_COMPRESSED_RGBA_ASTC_8x6 = 0x93B6
    val GL_COMPRESSED_RGBA_ASTC_8x8 = 0x93B7
    val GL_COMPRESSED_RGBA_ASTC_10x5 = 0x93B8
    val GL_COMPRESSED_RGBA_ASTC_10x6 = 0x93B9
    val GL_COMPRESSED_RGBA_ASTC_10x8 = 0x93BA
    val GL_COMPRESSED_RGBA_ASTC_10x10 = 0x93BB
    val GL_COMPRESSED_RGBA_ASTC_12x10 = 0x93BC
    val GL_COMPRESSED_RGBA_ASTC_12x12 = 0x93BD
    val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_4x4 = 0x93D0
    val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_5x4 = 0x93D1
    val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_5x5 = 0x93D2
    val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_6x5 = 0x93D3
    val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_6x6 = 0x93D4
    val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_8x5 = 0x93D5
    val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_8x6 = 0x93D6
    val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_8x8 = 0x93D7
    val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_10x5 = 0x93D8
    val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_10x6 = 0x93D9
    val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_10x8 = 0x93DA
    val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_10x10 = 0x93DB
    val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_12x10 = 0x93DC
    val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_12x12 = 0x93DD

    val GL_MULTISAMPLE = 32925
    val GL_LINE_SMOOTH = 2848
    val GL_RGBA8 = 32856
    val GL_COLOR = 6144
    val GL_BGRA = 32993
    val GL_UNSIGNED_INT_8_8_8_8_REV = 33639
}
