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
