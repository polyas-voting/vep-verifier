package de.polyas.core3.vep.verif

import java.io.File
import java.security.Provider
import java.security.Security

object VerifApp {

    init {
        Security.addProvider(org.bouncycastle.jce.provider.BouncyCastleProvider() as Provider?)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size !in 1 .. 2) {
            println("The program expects one required and a second optional parameter")
            println("First parameter (required): The folder name where the verification data files are placed")
            println("Second parameter (optional): A folder containing receipt PDFs to be checked")
        } else {
            val baseFolder = args[0]
            val receiptsFolder = args.getOrNull(1)
            if(checkFolder(baseFolder) && (receiptsFolder == null || checkFolder(receiptsFolder))) {
                try {
                    Verifier(baseFolder, receiptsFolder).performVerification()
                } catch (e: Exception) {
                    println("The verification process failed with the following error: ${e.message}")
                }
            }
        }
    }

    private fun checkFolder(folder: String): Boolean =
        File(folder).isDirectory.also { if (!it) println("$folder is not a valid/existing folder name") }
}
