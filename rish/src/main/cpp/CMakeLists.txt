cmake_minimum_required(VERSION 3.31)

project("rish")

set(CMAKE_CXX_STANDARD 17)

add_compile_options(-Werror=format -fdata-sections -ffunction-sections -fno-exceptions -fno-rtti -fno-threadsafe-statics)

if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    message("Building Release...")

    add_compile_options(-Os -flto -fvisibility=hidden -fvisibility-inlines-hidden)
    add_link_options(-flto -Wl,--exclude-libs,ALL -Wl,--gc-sections -Wl,--strip-all)
else ()
    message("Building Debug...")

    add_definitions(-DDEBUG)
endif ()

find_package(cxx REQUIRED CONFIG)

add_library(rish SHARED
        main.cpp
        pts.cpp
        rikka_rish_RishTerminal.cpp
        rikka_rish_RishHost.cpp)

target_link_libraries(rish log cxx::cxx)

if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    add_custom_command(TARGET rish POST_BUILD
            COMMAND ${CMAKE_STRIP} --remove-section=.comment "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/librish.so")
endif ()
