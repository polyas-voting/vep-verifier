package de.polyas.core3.vep.verif

import de.polyas.core3.vep.verif.input.Input
import de.polyas.core3.vep.verif.reporting.*
import de.polyas.core3.vep.verif.verification.*
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Verifier implementing the universal verification procedure for Polyas Core 3 VerifiableEP.
 *
 * @param baseFolderName Folder with the (unpacked) content of the verification package
 * @param receiptsFolderName Optional folder with voter's receipts (PDFs)
 * @param anonymousRevocations Only relevant for election with revocations. If true, the voters are revoked by
 *      their anonymous references; if false, by voter identifiers (in this case the system does not provide
 *      everlasting privacy, as the references are deterministically derived from voter identifiers).
 */
class Verifier(
    baseFolderName: String,
    private val receiptsFolderName: String?,
    private val anonymousRevocations: Boolean
) {
    private val baseFolder = File(baseFolderName)
    private val receiptsFolder = receiptsFolderName?.let { File(receiptsFolderName) }

    private val input = Input(baseFolder, receiptsFolder)

    fun performVerification() {

        checkBaseFolder()
        checkReceiptsFolder()
        println("\nGENERAL ELECTION DATA\n")
        showBallots(input.registry.ballotStructures)
        showRegistry(input.registry)

        val time = measureTimeMillis {
            if (input.secondDeviceParametersFingerprint != null) {
                verifying("the second device public parameters") {
                    SDParametersVerif.verify(input.registry, input.electionKey, input.secondDeviceParametersFingerprint)
                }
            } else {
                println("*** WARN: No file with the second device public parameters given")
            }

            verifying("the receipts") {
                ReceiptVerif.verify(input.registry, input::ballotBox, input::receipts)
            }

            verifying("the registry") {
                RegistryVerif.verify(input.registry)
            }

             verifying("the public election key") {
                 ElectionKeyVerif.verify(input.electionKey)
             }

             verifying("board ballot flagged") {
                 BallotFlaggedVerif(input.registry, anonymousRevocations = anonymousRevocations).verify(
                     input.electionKey,
                     input::ballotBox,
                     input::ballotFlagged,
                     input::revocationTokens,
                 )
             }

             verifying("ballot-incorrect") {
                 BallotIncorrectVerif.verify(input::ballotFlagged, input::ballotFilteredOut)
             }

             verifying("mix packets") {
                 MixInputPacketsVerif.verify(input.registry, input::ballotFlagged, input::inputMixPackets)
             }

             verifying("output mix packets") {
                 MixOutputPacketsVerif.verify(
                     input.registry,
                     input.electionKey,
                     input::inputMixPackets,
                     input::outputMixPackets
                 )
             }

             verifying("decrypted packets") {
                 DecryptedPacketsVerif.verify(input.electionKey, input::outputMixPackets, input::decryptedPackets)
             }
        }

        println("\n*************************************************************")
        println("Verification completed successfully [in ${timeString(time)}]")
        println("*************************************************************")

        println()
        println("TURNOUT INFORMATION:")
        reportTurnout(input::ballotFlagged)
        println()

        // Count the result
        showResult(input.registry, input::decryptedPackets)
    }

    private fun checkReceiptsFolder() {
        if (receiptsFolder == null) {
            println("No folder with receipts given. Therefore NO receipts will be checked.")
        } else {
            require(receiptsFolder.exists() && receiptsFolder.isDirectory) {
                "The receipt folder $receiptsFolder does not exist"
            }
        }
    }

    private fun checkBaseFolder() {
        require(baseFolder.isDirectory) { "$baseFolder is not a valid/existing folder" }
        println("Verification of election data located in $baseFolder")
        println("(full path = ${baseFolder.absoluteFile})\n")
    }

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

    private fun timeString(timeInMs: Long) = if (timeInMs<10000) "$timeInMs ms" else "${(timeInMs+500)/1000} s"
}