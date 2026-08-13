# Release

Publishing is fully automated via GitHub Actions (`.github/workflows/publish.yml`).
Pushing a `v*` tag builds both variants, runs the test suites, signs with GPG,
publishes to Maven Central, and creates a GitHub Release.

## Repository secrets (set once)

| Secret | Value |
| ------ | ----- |
| `GPG_PRIVATE_KEY` | Armored private key: `gpg --export-secret-key --armor <KEY_ID>` |
| `GPG_PASSPHRASE` | The GPG key passphrase |
| `CENTRAL_TOKEN_USER` | Central Portal user-token username |
| `CENTRAL_TOKEN_PASS` | Central Portal user-token password |

Set them under Settings → Secrets and variables → Actions.

## Steps to release

1. Bump the version in the three `pom.xml` files (parent + both modules).
2. Commit and push to `main`.
3. Tag and push:

   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```

4. Watch the run under Actions → "Publish to Maven Central". Once green, the
   artifacts are on Maven Central and the GitHub Release is created.
