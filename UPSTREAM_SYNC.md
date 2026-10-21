# Upstream sync — 2026-10-21

Reviewed upstream [`5.2.2-r2` through `5.2.2-r8`](https://github.com/V5-Client/V5Loader/compare/5.2.2-r1...5.2.2-r8), inclusive, against `dev` at `c4f5869dd9f4b27ec38e0f946b644d28e3a1a626`. The reviewed upstream tip is `4e9dccc868d7bc251a699e3ec65a504de40b5e52`, also the current `upstream/26.1.2` tip. This range requires no runtime or API changes in the offline loader.

| Upstream release | Decision |
| --- | --- |
| `5.2.2-r2` (`46f94e7`) | HTTP helper deduplication affects `V5Http`, which is absent from the offline loader. |
| `5.2.2-r3` (`7a00ef6`) | The proxy-provider link targets the upstream proxy manager, which is absent from this fork. |
| `5.2.2-r4` (`285a9b6`) | Keep local build/release workflows; do not add CommitBot or Discord notification credentials. |
| `5.2.2-r5` (`232dae8`) | Deferred automatic loader updates affect the removed `V5Loader`/`ModLoaderUpdater` path. Updates remain manual. |
| `5.2.2-r6` (`b663685`, `6f3f09c`) | Cloudflare publishing and Discord notification workflows are already absent. Keep building JNI from source rather than importing upstream binaries. |
| `5.2.2-r7`, `r8` (`1786273`, `4e9dccc`) | The Better Block Entities incompatibility declaration was added and then reverted upstream. Retain the final upstream behavior without adding that restriction. |

Minecraft 26.1.2/26.2, NanoVG, offline startup, local script loading, native builds, and generated typings remain unchanged. The companion scripts update through `5.2.2-r12` uses APIs already available in this `dev` loader.

## Validation

- `./gradlew build --no-daemon --console=plain --max-workers=4` succeeded with JDK 25 for both Minecraft 26.1.2 and 26.2, producing the corresponding `V5-Offline-DEV-2.1.0-offline-<minecraft>.jar` files under `versions/<minecraft>/build/libs/`.
- `git diff --check` passed. No loader API or native source changes were needed, so generated typings and native sources were not changed.
- The build reports existing dependency-version, documentation metadata, Kotlin deprecation/context-receiver, and Gradle deprecation warnings. Game startup and the companion scripts' gameplay behavior were not exercised.

---

# Upstream sync — 2026-10-02

Ported selected changes from upstream [`5.2.1-r4` through `5.2.2-r1`](https://github.com/V5-Client/V5Loader/compare/5.2.1-r3...5.2.2-r1), inclusive, onto `dev` at `f4ac6aac1c890abe9a56f0334323f8054c64c5aa`. The reviewed upstream tip is `6b0c298b762e652e0c67cfa4d801fc84d3977f47`.

| Upstream release | Ported changes |
| --- | --- |
| `5.2.1-r4` (`e7847b9`, `a165c2b`, `debe212`) | Android ARM64/x86_64 detection, NDK JNI build, and Linux/Windows ARM64 native builders. |
| `5.2.1-r5` (`e5ca37f`) | Clipboard copies use Minecraft's clipboard API through `Client.copy`, replacing AWT clipboard access. |
| `5.2.1-r6` (`fb9dee9`) | Cached structure results, initial scanning of loaded chunks, packet-triggered rescans, additional structure patterns, and batch text rendering optimization. |

## Offline adaptations

- Keep Minecraft 26.1.2/26.2, the offline startup/update behavior, NanoVG renderer, existing pathfinding fixes, and static MSVC runtime. Do not import upstream Skija downloads or updater changes; skip version-only `5.2.2-r1`.
- Package desktop NanoVG natives for Windows/Linux/macOS on x86_64 and ARM64. Compile Android NanoVG from pinned LWJGL 3.4.1 sources matching the Java bindings; prepend its extracted library directory while preserving the launcher's other LWJGL paths. Android never loads Linux/glibc NanoVG binaries or downloads natives at startup.
- Build Android with NDK 27.2.12479018, API 24, static libc++, and 16 KB page support. Build/release workflows collect all eight JNI targets and both Android NanoVG libraries. Generated binaries remain ignored by Git.
- Correct the two CI setup failures found during validation: invoke Android's preinstalled `sdkmanager` by its full SDK path, and use available Temurin 21 JNI headers/import libraries on Windows ARM64. Minecraft compilation and runtime remain on Java 25.
- Preserve single-block update rescans in `StructureFinder`; capture and check the world generation before queuing and publishing scans so work from a previous world cannot overwrite new results or remove a newer pending scan.
- Regenerate typings using `./gradlew generateTypings` and copy the generated 26.2 declarations to the scripts repository. This also removes stale declarations for online-only APIs already absent from the offline runtime. Neither declaration file is hand-edited.
- Ship this Loader together with the corresponding scripts update: Structure ESP now consumes `setActive`/`getRenderStructures` and `FoundStructure` rather than the removed submit/parallel-array/shutdown API.

## Validation

- `./gradlew generateTypings` and `./gradlew build` succeeded with JDK 25 for both Minecraft 26.1.2 and 26.2. A temporary Gradle init script used the public Google Maven Central mirror because the default endpoint returned HTTP 429; project repository configuration is unchanged.
- CMake built Linux x86_64 Pathfinder JNI and NanoVG, plus Android ARM64 and x86_64 Pathfinder JNI/NanoVG using the pinned NDK/LWJGL inputs. Android ELF inspection confirmed system-only dynamic dependencies and 16 KB segment alignment.
- actionlint 1.7.12 accepted all changed workflows (`-shellcheck=`); the full Windows/Linux ARM64/macOS builder matrix still runs in GitHub Actions after push.
- Local mod JARs include Linux x86_64 and both Android targets. The complete eight-platform JNI bundle is assembled by CI. Android launchers still need Java 25, compatible LWJGL core libraries, and OpenGL 3 support; device startup, rendering, and gameplay are not validated by compilation.

---

# Upstream sync — 2026-09-26

Reviewed upstream [`5.2.0-r3` through `5.2.1-r3`](https://github.com/V5-Client/V5Loader/compare/5.2.0-r2...5.2.1-r3), inclusive. The reviewed upstream tip is `4eefc9dbaf6a23d54c95a21ad725733948d65990`; the offline base is `d0303cefd57f315a8d17fed0ce3ae9ef84ea917e`.

| Upstream release | Decision |
| --- | --- |
| `5.2.0-r3` | Skija shader-resource leak fix is not applicable to the offline NanoVG renderer. |
| `5.2.0-r4` | Skija font/typeface cache limits are not applicable to the offline NanoVG renderer. |
| `5.2.0-r5`, `5.2.1-r1` | Retain Minecraft 26.1.2/26.2 and the offline version. Do not import the 26.3 input, graphics, dependency, online-loader, or release-workflow changes. |
| `5.2.1-r2` | Port render-camera synchronization and run the 26.2 post-world render trigger before gizmo finalization. Tracers now use the camera state of the frame being rendered. |
| `5.2.1-r3` | Defer the Skija PiP/HUD texture-cache pipeline; its renderer and native resources do not exist in this fork. |

The port retains the existing 26.1.2 render hook and the NanoVG backend. No exported script API changes are required. It does not replace the offline game-state, disconnect, profile, pathfinding, or release behavior.

Validation: reviewed both Stonecutter branches and ran `./gradlew build`. The local build could not start because the Gradle distribution download was unreachable; the available local JDK is 17 rather than the required 25. The existing GitHub Actions build remains the compilation gate for both supported Minecraft targets. In-game rendering still requires client validation.
