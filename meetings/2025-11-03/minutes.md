## Attendees

- Robert Walworth
- Ivan Asenov
- Keith Kowal
- Sophie Bulloch

## Minutes

- Rob introduced the SDK v3 sandbox
  - Folder in the SDK collaboration hub to come up with ideas and suggestions for what a v3 SDK will look like
  - Invited discussion and contributions to the documents and code that currently lives there
  - Keith brought up questions about the need for a v3 SDK
    - Rob mentioned a strong desire to redesign some of the SDK APIs to allow for less user error and better intuitive use, as well as clean the slate of deprecated APIs
- Rob went through current open PRs for design doc API guidelines
  - Described what is already there and approved, as well as the improvements being made
    - Adding extra type annotations
    - "extends" keyword for inherited types
- Ivan went over his design doc for HIP-1313
  - He brought up a concern about defining the high volume throttle boolean flag in the base `Transaction`, or only doing it in the transactions where high-volume throttles are enabled
    - Rob and Ivan both agreed it should be defined at the `Transaction` level, so that enabling transactions later for high-volume throttles wouldn't require an SDK upgrade
  - Testing this HIP brought about questions as to how to test without introducing flakiness to SDK CI workflows
    - Testing based on speed can be tough because some CI runners may experience heavier traffic at different times, therefore introducing flakiness
    - The idea of a custom build was brought up, but that would require a separate CI job
    - The HIP is still a couple of months away from being introduced, so there's time to figure this out
      - Team will wait until consensus node has made more progress on this