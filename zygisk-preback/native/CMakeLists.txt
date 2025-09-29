cmake_minimum_required(VERSION 3.18)
project(preback_zygisk LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(zygisk-preback SHARED
    src/module.cpp
)

target_include_directories(zygisk-preback
    PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_compile_definitions(zygisk-preback
    PRIVATE
        __ANDROID__
)

target_link_libraries(zygisk-preback
    PRIVATE
        log
)
