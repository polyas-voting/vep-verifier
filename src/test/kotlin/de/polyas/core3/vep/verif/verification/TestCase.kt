package de.polyas.core3.vep.verif.verification

data class TestCase<TD>(
    val testCaseName: String,
    val testDataType: String,
    val testData: TD,
    val expectedResult: String
)