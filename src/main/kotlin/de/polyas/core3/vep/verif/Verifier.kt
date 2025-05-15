package de.polyas.core3.vep.verif

import de.polyas.core3.vep.verif.input.Input
import de.polyas.core3.vep.verif.reporting.showBallots
import de.polyas.core3.vep.verif.reporting.showRegistry
import de.polyas.core3.vep.verif.verification.BallotIncorrectVerif
import de.polyas.core3.vep.verif.verification.ElectionKeyVerif
import de.polyas.core3.vep.verif.verification.MixInputPacketsVerif
import de.polyas.core3.vep.verif.verification.RegistryVerif
import java.io.File
import kotlin.system.measureTimeMillis

class Verifier(baseFolderName: String) {

    private val baseFolder = File(baseFolderName).also {
        require(it.isDirectory) { "$it is not a valid/existing folder" }
    }

    private val input = Input(baseFolder)

    fun performVerification() {

        checkAndReportBaseFolder()

        // val receiptsFile = receiptsFileName
        //     ?.let { File(receiptsFileName) }
        //     ?.also { require(it.exists() && it.isDirectory) { "The receipt folder $receiptsFileName does not exist" } }
        //     .ifNull { println("No folder with receipts given. Therefore NO receipts will be checked.") }

        println("\nGENERAL ELECTION DATA\n")

        // Display the ballots and the registry
        showBallots(input.registry.ballotStructures)
        showRegistry(input.registry)

        val time = measureTimeMillis {

            // verifying("The board hash chains") {
            //     ExportIntegrity.checkBoards(baseFolder.toPath())
            // }

            // if (input.secondDeviceParametersFingerprint != null) {
            //     verifying("the second device public parameters") {
            //         SDParametersVerif.verify(input.registry, input.electionKey, input.secondDeviceParametersFingerprint)
            //     }
            // } else {
            //     println("*** WARN: No file with the second device public parameters given")
            // }

            // verifying("the receipts") {
            //     ReceiptVerif.verify(input.registry, input::ballotBox, input::receipts)
            // }

            verifying("the registry") {
                RegistryVerif.verify(input.registry)
            }

             verifying("the public election key") {
                 ElectionKeyVerif.verify(input.electionKey)
             }

            // verifying("board ballot flagged") {
            //     BallotFlaggedVerif.verify(
            //         input.electionKey,
            //         input.registry,
            //         input::ballotBox,
            //         input::ballotFlagged,
            //         input::revocationTokens,
            //     )
            // }

             verifying("ballot-incorrect") {
                 BallotIncorrectVerif.verify(input::ballotFlagged, input::ballotFilteredOut)
             }

             verifying("mix packets") {
                 MixInputPacketsVerif.verify(input.registry, input::ballotFlagged, input::inputMixPackets)
             }

            // verifying("output mix packets") {
            //     MixOutputPacketsVerif.verify(
            //         input.registry,
            //         input.electionKey,
            //         input::inputMixPackets,
            //         input::outputMixPackets
            //     )
            // }

            // verifying("decrypted packets") {
            //     DecryptedPacketsVerif.verify(input.electionKey, input::outputMixPackets, input::decryptedPackets)
            // }
        }

        println("\n*************************************************************")
        println("Verification completed successfully [in ${timeString(time)}]")
        println("*************************************************************")

        // println()
        // println("TURNOUT INFORMATION:")
        // reportTurnout(input::ballotFlagged, input.registry)
        // println()

        // Count the result
        // Counting(input.registry).counting(input.decryptedPackets())
    }

    private fun checkAndReportBaseFolder() {
        require(baseFolder.isDirectory) { "$baseFolder is not a valid/existing folder" }
        println("Verification of election data located in $baseFolder")
        println("(full path = ${baseFolder.absoluteFile})\n")
    }

    private fun timeString(timeInMs: Long) = if (timeInMs<10000) "$timeInMs ms" else "${(timeInMs+500)/1000} s"

    private inline fun verifying(verifiedElementName: String, block: () -> Unit) {
        println("[ Verifying $verifiedElementName...")
        try {
            val time = measureTimeMillis {block()}
            println("  Done in $time ms ] ")
        } catch (e: Exception) {
            println("\n********************************************************************************************")
            println("  Verification of $verifiedElementName failed with the following error:\n  ${e.message}")
            println()
            throw e
        }
    }

}