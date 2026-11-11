# F-Droid submission notes

The metadata draft in this folder is intended for the separate `fdroiddata` repository. F-Droid's official quick-start asks upstream projects to publish a tagged source release and maintain Fastlane metadata in the app repository.

Before submitting:

1. Upload this final `DeepCalc-Android` source to `PrasannaOne/DeepCalc` and let the Android CI workflow commit it.
2. Wait for the final source commit to pass `:app:testDebugUnitTest`, APK builds and AAB build.
3. Create the `v2.1.0` tag on that exact commit.
4. Copy `com.prasannaone.deepcalc.yml.template` into `fdroiddata/metadata/com.prasannaone.deepcalc.yml`.
5. Replace `REPLACE_WITH_FINAL_40_CHARACTER_COMMIT_SHA` with the full 40-character commit SHA that the `v2.1.0` tag points to. F-Droid metadata requires the full commit hash, not a branch or tag name.
6. Submit a merge request to `fdroiddata` and address any build-environment or licence-review feedback.

The F-Droid recipe disables ABI splits and builds one universal release APK. F-Droid signs the app with its own signing key. That signature is separate from any APK you distribute yourself, so users generally cannot update an upstream-signed install directly from the F-Droid-signed build without reinstalling.

The draft is not a guarantee of acceptance. F-Droid must review the software, licences, artwork, dependencies, build process and the exact tagged source. The recipe also needs a build environment with Gradle 9.5.0 or newer for Android Gradle Plugin 9.3.0.
