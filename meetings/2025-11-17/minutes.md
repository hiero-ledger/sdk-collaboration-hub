## Attendees

- Robert Walworth
- Ivan Asenov
- Keith Kowal
- Sophie Bulloch
- Angelina Cuppalini
- Venelin Vasilev
- Rohit Sinha

## Minutes

- SDK platform work
    - Rob updated that changes for JS SDK have been merged
    - Work in the Rust SDK will be put off until its next release
- v3 API Keys
  - Rob recapped previous week's discussion on keys
    - Trying to come up with v3 SDK APIs that allow for forward-compatibility and simplicity
  - Rohit confirmed that PKCS8 is the most popular way to encode keys with algorithm metadata
  - Rob expressed some confusion between current DER-encoding techniques used by the SDKs and PKCS8
  - Keith mentioned we should be using ASN.1 standard DER-encodings and that we should audit our current SDKs to make sure they follow this standard
    - Rob brought up they may be different, because keys generated in one SDK have not been recognized in another before
  - Keith expressed concerns about overlap between ECDSA and ED25519 private keys, stating one API wouldn't be able to differentiate
    - Rob confirmed this would only be able to be done with DER-encoded keys, otherwise the API would default to ECDSA (our most usual use case)
    - Rohit mentioned this would be fine as long as standardized DER-encodings (ASN.1, PKCS8) are used
  - Keith expressed more concerns about libraries out there that support the encodings we'd want
    - Rohit further cemented the idea that we should catalog our SDKs and determine the encodings we're using
  - Keith stated our encodings should be consistent with those across the web3 space (MetaMask, etc.) and that we maybe don't need to support lesser-used encodings
  - Keith mentioned adding more keys (e.g. quantum keys) down the line would require a lot of work and wouldn't be a surprise
    - Rob restated that the idea is to design the v3 API now so that we can support that event, and not have to do a v4 for instance
- SDK sample code guide
  - Rob was going to take Keith's google sheet and add something to the SDK hub for review
- Hacktoberfest findings
  - Sophie presented on her findings for Hacktoberfest with the Python SDK
    - Python SDK activity increased by 660% in October!
    - Several first time contributors
      - Mostly computer science students and graduates
    - Repo tags helped grab the attention of interested developers for a specific language
    - Current documentation helped steer developers but can be expanded
    - Training modules would be nice to help new developers understand SDKs, Hiero, etc.
      - In person meetups or videos
    - More maintainers are needed to help with scaling (more reviewers for PRs, etc.)
    - Automated bots to help with PR issues
      - DCO checks
      - Workflows passing/failing
      - Make sure examples still work
      - Make sure APIs match protobufs
  - Keith mentioned it might be a good idea for Hashgraph/Limechain to put a dedicated developer on the Python SDK as it grows in popularity
  - Keith suggested making this same presentation to the TSC