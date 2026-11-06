# Preserved Android release artifact

The cleanup on 2026-11-06 removes `androidApp/release/androidApp-release.aab`
from the Git index. The original file remains on disk. An identical local copy is
under `release-archives/android/3a7fe59491b8e0ab9019040cbb01cff57b745291e83f199b73bc52b65fb20361/androidApp-release.aab`.
That ignored directory also contains `manifest.json` and `SHA256SUMS`.

The archive records the existing binary, without rebuilding or changing it.
Its signing and distribution acceptance have not been revalidated.

- Source commit: `d081490e0be83e0ca3b4f561fa4d3bf22115a147`.
- Git blob: `4c0a6fa19c046ab46f618032f7b1b11b03131126`.
- Size: 11904346 bytes.
- SHA-256: `3a7fe59491b8e0ab9019040cbb01cff57b745291e83f199b73bc52b65fb20361`.

Verify the preserved local copy from its archive directory:

```sh
shasum -a 256 -c SHA256SUMS
```

The local archive is not distributed with a fresh clone. The original binary
remains in Git history and can be recovered without changing the working tree:

```sh
git show d081490:androidApp/release/androidApp-release.aab > /tmp/androidApp-release.aab
shasum -a 256 /tmp/androidApp-release.aab
```

Future release bundles and mappings belong in release storage or ignored local
archives. Shared IDE code styles and Xcode schemes remain versioned.
