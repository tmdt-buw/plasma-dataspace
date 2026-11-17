# PLASMA utilities

Shared Java utilities used by the PLASMA Maven reactor. This module does not have its own HTTP service or Docker deployment.

From the repository root with JDK 21 and Maven:

```sh
mvn -pl components/utilities -am test
```

Rebuild and test consuming services when changing common behavior. Use the [root README](../../README.md) for the complete SMS/DPS build and integration checks; building only this module does not verify all callers.
