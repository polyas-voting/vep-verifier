package de.polyas.core3.vep.verif.verification

import de.polyas.core3.vep.verif.types.PublicVoterId
import de.polyas.core3.vep.verif.types.Registry
import de.polyas.core3.vep.verif.types.Voter

/**
 * Verification of the registry board
 */
object RegistryVerif {

    fun verify(registry: Registry) {
        checkLength(registry)
        checkDuplicates(registry)
        checkPublicLabels(registry)
    }

    private fun checkLength(registry: Registry) {
        val registryLen = registry.voters.size
        val vidLen = registry.voterIdentifiers.size
        check(registryLen == vidLen) {
            "the size of the registry ($registryLen) does not match the number of public voter identifiers ($vidLen)"
        }
        println("  *** INFO: The number of registered voters = $registryLen")
    }

    private fun checkDuplicates(registry: Registry) {
        check(
            registry.voters.asSequence().map(Voter::id).containsNoDuplicates()
        ) { "the registry contains entries with the same voter id" }

        check(
            registry.voters.asSequence().map(Voter::cred).containsNoDuplicates()
        ) { "the registry contains duplicated entries with the same public credential" }

        check(
            registry.voterIdentifiers.asSequence().map(PublicVoterId::publicId).containsNoDuplicates()
        ) { "the registry contains duplicated entries with the same public voter identifier" }
    }

    private fun checkPublicLabels(registry: Registry) {
        // collect ballot identifiers
        val ballotIds: Set<String> = registry.ballotStructures.asSequence()
            .map { it.id }
            .toSet()

        // and check that each voter has a valid public label
        for (voter in registry.voters) {
            for (b in voter.publicLabel.split(":")) {
                check(b in ballotIds) { "voter ${voter.id} has invalid ballot id $b in the public label ${voter.publicLabel}" }
            }
        }
    }
}

