package spirite.consoleApp.fileValidation

import spirite.base.file.FileManager
import spirite.consoleApp.MasterContext
import spirite.sguiHybrid.Hybrid
import java.io.File
import javax.swing.SwingUtilities

// This suite does validation by loading files into Image Workspaces and then saves those workspaces to new files
class WorkspaceBackedFileValidation {
    var bufferFileLocation : File = File("E:\\Bucket\\sif")
    val context = MasterContext()

    init {
        context.init()
    }

    fun run( folder: File, outputFile: File) {

        if( outputFile.exists())
            outputFile.delete()
        outputFile.createNewFile()
        val sb = StringBuilder()

        fun runOnFile(file: File) {
            println(file.absolutePath)
            if( file.isDirectory ) {
                for (listFile in file.listFiles())
                    runOnFile(listFile)
            }
            else {
                if( file.extension.toLowerCase() in setOf("sif", "sif~")) {
                    doTest(file, sb)
                    sb.appendln()
                }
            }
        }

        runOnFile(folder)


        outputFile.writeText(sb.toString())
    }

    private fun doTest(file: File, sb: StringBuilder) {
            println(file.canonicalPath)
            sb.appendln(file.canonicalPath)

            val oldFile =
                File(PureSifFileValidation.bufferFileLocation.canonicalPath + "\\" + "fileValidationTest-old.sif")
            val newFile =
                File(PureSifFileValidation.bufferFileLocation.canonicalPath + "\\" + "fileValidationTest-new.sif")

            SwingUtilities.invokeAndWait {
                Hybrid.gle.runInGLContext {
                    loadThenSave(file, oldFile, false, sb)
                    loadThenSave(file, newFile, true, sb)
                }
            }

            PureSifFileValidation.binCompare(oldFile, newFile, sb)
    }

    private fun loadThenSave(inFile: File, outFile: File, new: Boolean, sb: StringBuilder) {
        try {
            FileManager.v2Load = new
            FileManager.v2Save = new

            context.master.fileManager.openFile(inFile)
            val ws = context.master.workspaceSet.currentMWorkspace!!
            context.master.fileManager.saveWorkspace(ws!!, outFile)
            context.master.workspaceSet.removeWorkspace(ws)
        }catch (e: Throwable) {
            sb.appendln("Failed on New = $new for reason: ${e.message}")
        }
    }

}