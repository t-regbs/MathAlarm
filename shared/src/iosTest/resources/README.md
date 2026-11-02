# Legacy iOS database fixture

`legacy-v9-native.sqlite3` contains one disabled `M8_upgrade_preserved` alarm,
Room schema version 9 and its original identity hash. It comes from the disposable
iPhone 17 / iOS 27.0 native build 20 → 21 upgrade validation on 3 October 2026,
when iOS still used bundled SQLite 2.7.1. The database/WAL was materialized as a
standalone file using SQLite backup. It contains no physical-device/user data.

The iOS-only regression copies it to a unique temporary file, opens it through
the production database factory, verifies the entire legacy row, then persists
an unresolved recurring occurrence and reopens it. It checks that changing to
system SQLite preserves schema, IDs, text, challenge settings and durable fields.
The original fixture is read-only input and never used as the app database.
