This is the kiebabo library - the Kotlin Is Embarassingly Bad at Binary Operations library

Seriously, screw you, Kotlin.  I know you think anyone who works with binary data is a neckbearded mouthbreather
stroking their pocket protecter while modifying assembly with tweezers in a clean room, but you shouldn't have to
write a DLL in order to do basic data manipulation.  No, it's not acceptable to just wave it away and say "that's
not Kotlin's use case."  That's be like saying "SQL isn't this language's use case."  Data is not your programming
languages use case?  Say I'm reading in a file and it has a color stored in ARGB format and I need it to be in RGBA
format.  You read in 4 bytes into a ByteArray.  So how do you turn that into an int (which is a completely reasonable
thing for a graphics library to ask)?

In a sane language, it would be
val colorData = (raw[0] << 8) | (raw[1] << 16) | (raw[2] << 24) | (raw[3])

And kotlin doesn't support << and |.  Annoying, but whatever.
So is it:
val colorData = (raw[0] shl 8) or (raw[1] shl 16) or (raw[2] shl 24) or (raw[3] )

Nope, you can't use bitwise operators on bytes and implicit type elevation is taboo on Kotlin because they've imagined
a single scenario where that could be dangerous so fuck the million use cases where it is a simple convenience expected
of programming languages.  Whatever, so this is it, right?
val colorData = (raw[0].toInt() shl 8) or
        (raw[1].toInt() shl 16) or
        (raw[2].toInt() shl 24) or
        (raw[3].toInt())

Nope, because Kotlin's hatred of all things data means that Bytes are signed and converting the Byte 0xff will convert to
the Int 0xfffff81 and everything becomes wrong.  So what's the correct code?
val colorData = (raw[0].toUByte().ToUInt shl 8) or
        (raw[1].toUByte.toUInt() shl 16) or
        (raw[2].toUByte.toUInt() shl 24) or
        (raw[3].toUByte.toUInt())

See, you can't just convert toUInt, because if you take the Byte 0xff and convert it to UInt it becomes 4294967169.  Why?
Because 0xff is -127 and -127 in Int which is 0xfffff81 and what's that in UByte form?  4294967169

GOOD.  GOD.  I can only imagine how much unnecessary conversion overhead is going on under the hood.  It's just.
embarrassing.  It's completely embarrassing and it honestly makes me embarrassed to be using this language.