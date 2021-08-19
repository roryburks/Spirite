package spirite.consoleApp

import spirite.consoleApp.fileValidation.WorkspaceBackedFileValidation
import java.io.File

fun main( args: Array<String>) {
//    val len = WorkspaceBackedFileValidation().getSifFiles(File("x")).count()
//    println(len)

    WorkspaceBackedFileValidation().run(
        File("x"),
        File("E:\\Bucket\\sif\\run-3.txt"),
        0, null)

    println("done")



}
