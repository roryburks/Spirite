package spirite.consoleApp

import spirite.consoleApp.fileValidation.PureSifFileValidation
import java.io.File
import java.lang.StringBuilder

fun main( args: Array<String>) {
    val sb = StringBuilder()
    PureSifFileValidation.validateFile(File("NA"),sb)

    val string = sb.toString()
    print("brk")



}
