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
