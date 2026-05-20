# Universal Verifier for POLYAS Core 3 Verifiable EP

This repository contains a reference implementation of the universal
verifiability procedure for the POLYAS Core 3 Verifiable EP (Enhanced Privacy)
system.

For the system documentation, see
https://github.com/polyas-voting/core3-verifiable-doc.


## Prerequisites

- **Java 25** or higher

- **[polyas-core3-crypto](https://github.com/polyas-voting/polyas-core3-crypto)**
  (specifically the `crypto-elgamal` module) used as dependency — you may need to clone this
  repository and install it into your local Maven repository (via `mvn install`) first.
  Please, make sure you install the version referenced in `pom.xml`.


## Building and Testing

Build the project and run all tests with:

```sh
mvn install
```

This also produces an executable JAR with all dependencies in the `target/`
folder, ready to use as described below.


## Usage

Run the verifier with:

```sh
java -jar target/vep-verifier-<VERSION>-jar-with-dependencies.jar <verification-data-folder> [receipts-folder]
```

**Arguments:**

- `<verification-data-folder>` — path to the unpacked election data package.
  This folder may additionally contain a
  `secondDeviceParametersFingerprint.json` file with the second device public
  parameters fingerprint.

- `[receipts-folder]` *(optional)* — path to a folder containing voter receipt
  PDFs (from either the first or second device).

**Examples** using the included sample data:

```sh
# Verify election data only
java -jar target/vep-verifier-<VERSION>-jar-with-dependencies.jar test-data/small-no-revocations

# Verify election data and receipts
java -jar target/vep-verifier-<VERSION>-jar-with-dependencies.jar test-data/small-no-revocations test-data/small-no-revocations/receipts
```
