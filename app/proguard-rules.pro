# Dayline release shrinking rules.
# Room, WorkManager, Compose, DataStore and AndroidX ship their own consumer rules,
# and the app uses no reflection or serialization libraries of its own, so nothing
# extra needs keeping today. Add rules here if a release-only crash points at R8.
