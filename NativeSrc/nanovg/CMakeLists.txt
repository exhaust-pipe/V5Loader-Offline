cmake_minimum_required(VERSION 3.13)
project(V5NanoVG LANGUAGES C)

set(V5_LWJGL_SOURCE_DIR "" CACHE PATH "LWJGL 3.4.1 source checkout")
set(LWJGL_MODULES "${V5_LWJGL_SOURCE_DIR}/modules/lwjgl")
if(NOT EXISTS "${LWJGL_MODULES}/nanovg/src/generated/c/org_lwjgl_nanovg_LibNanoVG.c")
  message(FATAL_ERROR "Set V5_LWJGL_SOURCE_DIR to the LWJGL checkout matching gradle/libs.versions.toml")
endif()

add_library(lwjgl_nanovg SHARED
  "${LWJGL_MODULES}/nanovg/src/generated/c/org_lwjgl_nanovg_LibNanoVG.c"
  "${LWJGL_MODULES}/nanovg/src/generated/c/org_lwjgl_nanovg_NanoVG.c"
  "${LWJGL_MODULES}/nanovg/src/generated/c/org_lwjgl_nanovg_NanoVGGL2.c"
  "${LWJGL_MODULES}/nanovg/src/generated/c/org_lwjgl_nanovg_NanoVGGL3.c"
  "${LWJGL_MODULES}/nanovg/src/generated/c/org_lwjgl_nanovg_NanoVGGLES2.c"
  "${LWJGL_MODULES}/nanovg/src/generated/c/org_lwjgl_nanovg_NanoVGGLES3.c"
  "${LWJGL_MODULES}/nanovg/src/generated/c/org_lwjgl_nanovg_NanoSVG.c"
)
target_include_directories(lwjgl_nanovg PRIVATE
  "${LWJGL_MODULES}/core/src/main/c"
  "${LWJGL_MODULES}/core/src/main/c/linux"
  "${LWJGL_MODULES}/nanovg/src/main/c"
  "${LWJGL_MODULES}/stb/src/main/c"
)
if(NOT ANDROID)
  find_package(JNI REQUIRED)
  target_include_directories(lwjgl_nanovg PRIVATE ${JNI_INCLUDE_DIRS})
endif()
target_compile_definitions(lwjgl_nanovg PRIVATE LWJGL_LINUX)
target_link_libraries(lwjgl_nanovg PRIVATE m)
set_target_properties(lwjgl_nanovg PROPERTIES C_VISIBILITY_PRESET hidden)
