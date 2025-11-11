## Attendees

- Robert Walworth
- Ivan Asenov
- Keith Kowal
- Sophie Bulloch
- Hendrik Ebbers
- Roger Barker
- Angelina Cuppalini
- Venelin Vasilev

## Minutes

- Agenda location
  - Rob asked if the SDK meeting agenda was going to move to the governance repo like previously discussed
  - Hendrik mentioned Jessica created a folder in governance for other meetings' agendas so we should start putting agendas there
  - Rob was going to reach out to Jessica to get this setup and confirmed
- Upcoming SDK platform work
  - Roger and platform team is going to be doing work in the SDKs this week to flip the order of publishing
    - Publish to hiero-ledger first before hashgraph
  - Rob brought up concerns about breaking changes, but Roger said SDKs should operate the same as they have
  - There was confusion on dates (Roger said Nov. 13th - 14th, but Hashgraph calendar event says Nov. 12th - 13th)
  - Roger to coordinate with SDK teams on when this will happen
- v3 API Keys
  - Hendrik presented a proposed implementation for keys for v3, which involves defining the key algorithm and encoding explicitly
  - Hendrik asked how the strict the SDK should be about this, or if there's a way for SDKs to determine the key algorithm and encoding from an input string
  - Rob mentioned SDKs should be able to determine the key algorithm if DER-encoded
  - Keith expressed concerns about the proposal, stating key creation and usage should be as simple as possible for users who we shouldn't expect to be cryptography experts
  - Hendrik agreed, but stated that it would be hard to keep it simple as more keys get added to the network down the line
  - Hendrik stated an option that involved keeping a default algorithm and encoding, that would cover most cases
    - This would involve looking at metrics, determining most common use cases, etc.
  - Keith proposed to stop using DER-encoding since no other web3 network uses it and it is very confusing
    - Hendrik brought up the Hedera portal, which has a bunch of different boxes for different encodings of the same key (raw, DER, 0x, etc.)
  - Rob mentioned SDKs use DER-encoding to determine what algorithm a key uses
    - There was some confusion on if SDKs currently do this or not
  - Hendrik proposed creating enums for algorithm and encodings, which will prevent SDKs from having to develop many new methods for the different algorithms and encodings going forward
  - Further discussion about what encodings the SDKs support
    - What is differences between raw, DER, hex, 0x, etc.?
  - Ivan proposed asking community developers about use cases and get some feedback there
  - Rob proposed inviting Rohit (Hashgraph head of cryptography) to the next meeting to get more input
  - Discussion somewhat settled on two methods:
    - `String create(String bytes)`: input DER-encoded key string to determine algorithm and create key. Otherwise, default to ECDSA key (since this is what we're trying to push developers to use)
    - `String create(String bytes, KeyAlgorithm alg, KeyEncoding enc)`: input all needed parameters for no ambiguity and create key
- SDK example code guide
  - Keith presented a document that aims to standardize how examples look across all SDKs
    - Usage of `.env` files, common variable names, how a network is setup, step comments, etc.
    - Will help AI be able to read and understand examples better
  - Keith to share link to google doc with specifications