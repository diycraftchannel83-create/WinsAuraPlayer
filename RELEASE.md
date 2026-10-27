# WinsAuraPlayer Android release

## Build and publish

1. Add repository Actions secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_STORE_PASSWORD`, `ANDROID_KEY_PASSWORD`, and `ANDROID_KEY_ALIAS`.
2. Open **Actions → Build and Publish WinsAuraPlayer** and run **Run workflow** to verify the release build artifacts.
3. To publish APK and AAB files on the GitHub Releases page, create and push a version tag beginning with `v`, for example `v1.0.0`. The workflow will build both release outputs and attach the APK, AAB, and SHA-256 checksums to that release.

Never commit the keystore or signing passwords into the repository.