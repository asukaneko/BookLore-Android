# BookLore Android

Native Android client for [BookLore](https://github.com/booklore-app/booklore), built with Kotlin and Jetpack Compose. The application connects to a user-hosted BookLore server; it does not replace or embed the BookLore web UI.

## Project status

The first native Android MVP is implemented and a signed release APK is available at [app-release.apk](/C:/Users/ycssb/Desktop/document/code/BookLore-Android/app/build/outputs/apk/release/app-release.apk).

It supports one self-hosted server, HTTPS-by-default connection validation, encrypted local sessions, local-credential login with rotated refresh tokens, library/shelf browsing, authenticated cached cover display with full-title fallback covers, search, book details, reading-status updates, personal ratings, and native EPUB and PDF readers. EPUB opens from an authenticated temporary cache, restores CFI/HREF positions, synchronizes progress with BookLore, and provides contents, theme, working font-size, font-family, and page-margin controls. PDF uses Android's native `PdfRenderer`, restores its one-based page position, synchronizes page progress with BookLore, and persists a choice between horizontal paginated reading and vertically continuous reading. Both reader formats support center-tap immersive mode and use private temporary storage rather than an offline library. English and Simplified Chinese are included.

Native CBX/audiobook readers, EPUB annotations, OIDC sign-in, Room offline cache, and background sync remain subsequent milestones. PDF zoom, pan, rotation, annotations, and reading-session upload also remain planned work. See [MVP delivery status](docs/MVP_STATUS.md) for precise scope and evidence.

## Documentation

- [Project build plan](docs/PROJECT_BUILD_PLAN.md): scope, architecture, milestones, acceptance criteria, testing, and localization.
- [BookLore web API reference](docs/WEB_API_REFERENCE.md): Android-facing contract notes and upstream endpoint index.

## Upstream baseline

The documentation was reviewed against BookLore `develop` commit `fd06c1b6db116690d923b918767a59699e2721db` (2026-06-26). BookLore is self-hosted and normally serves HTTP on port `6060`; deploy it with HTTPS before using it outside a trusted local network.

The upstream project is licensed under AGPL-3.0. Client licensing, name/logo permission, and dependency compatibility must be approved before a public release. The included APK is signed with a local development release key excluded from Git; replace and securely back up this key before public distribution.
