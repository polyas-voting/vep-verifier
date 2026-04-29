package de.polyas.core3.vep.verif.verification

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.polyas.core3.crypto.elgamal.VerificationResult
import de.polyas.core3.crypto.elgamal.instance.ECElement
import de.polyas.core3.crypto.elgamal.instance.EllipticCurveInst
import de.polyas.core3.vep.verif.types.Ballot
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import kotlin.test.assertTrue

class BallotVerifierTest {

    private val objectMapper = jacksonObjectMapper()
    private val group = EllipticCurveInst()
    private val verifier = BallotVerifier(group)

    data class BallotVerificationInput(
        val publicKey: ECElement,
        val publicCredential: ECElement,
        val ballot: Ballot<ECElement>
    )

    private fun verifyBallot(publicKey: ECElement, publicCredential: ECElement, ballot: Ballot<ECElement>): VerificationResult {
        return verifier.verifyZKP(
            publicKey, publicCredential, ballot
        )
    }

    @TestFactory
    fun `ballot verification test cases`(): List<DynamicTest> {
        val testCaseDir = File("test-data/test-cases-ballot-verif")
        val testFiles = testCaseDir.listFiles { f -> f.extension == "json" }
            ?: error("No test case files found in $testCaseDir")

        return testFiles.map { file ->
            val testCase = objectMapper.readValue<TestCase<BallotVerificationInput>>(file)
            DynamicTest.dynamicTest(testCase.testCaseName) {
                val result = verifyBallot(
                    testCase.testData.publicKey,
                    testCase.testData.publicCredential,
                    testCase.testData.ballot
                )

                val expectSuccess = testCase.expectedResult == "Success"
                if (expectSuccess) {
                    assertTrue(result is VerificationResult.Correct,
                        "[${testCase.testCaseName}] Expected success but got failure: ${(result as? VerificationResult.Failed)?.errorMessage}")
                } else {
                    assertTrue(result is VerificationResult.Failed,
                        "[${testCase.testCaseName}] Expected failure but got success")
                }
            }
        }
    }
}
