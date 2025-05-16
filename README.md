# Verification tool for POLYAS Core 3 Verifiable EP (Enhanced Privacy)

This repository contains an implementation of the universal
verifiability procedure for the Core 3 Verifiable EP (Enhanced Privacy)
system.


## Building and testing

For building and testing run

```sh
mvn install
```

This will also create a JAR with dependencies (in the `target` folder) which
can be used as described in the next step


## Usage

To run the verification application, execute

```
java -jar target/vep-verifier-<VERSION>-jar-with-dependencies.jar <verification-data-folder> <receipt-folder>
```

where 

* `<verification-data-folder>` is a folder where the (unpacked) files from an
   election data package are located. This folder can, additionally, contain
 
     - file `secondDeviceParametersFingerprint.json` with the second device public parameters fingerprint,

* `receipts-folder` is optional and, when provided, should contain receipt PDFs of
   voters which may originate from the voter's first or second device.


You can try, for instance, the included sample test data:

```bash
java -jar target/vep-verifier-0.1.1-SNAPSHOT-jar-with-dependencies.jar test-data/small-no-revocations test-data/small-no-revocations/receipts
```
