package spirite.consoleApp

import spirite.consoleApp.fileValidation.PureSifFileValidation
import spirite.consoleApp.fileValidation.WorkspaceBackedFileValidation
import java.io.File
import java.lang.StringBuilder

fun main( args: Array<String>) {
//    val len = WorkspaceBackedFileValidation().getSifFiles(File("x")).count()
//    println(len)

    WorkspaceBackedFileValidation().run(
        File("x"),
        File("E:\\Bucket\\sif\\run-3.txt"),
        0, null)

    println("done")



}
