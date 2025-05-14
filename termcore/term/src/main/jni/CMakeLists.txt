##
# Copyright (C) 2019-2024 Roumen Petrov.  All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##

cmake_minimum_required(VERSION 3.10)
project(term-c)

add_executable(
        libexec-t1plus.so
        appcmd.c
        socket.c
        atomicio.c
)
target_compile_definitions(
        libexec-t1plus.so
        PRIVATE
        PACKAGE_NAME="${APPLICATION_ID}"
)
target_compile_options(
        libexec-t1plus.so
        PRIVATE
        -fPIC
        # build without fortify, compatibility with SDK < 17!
        -U_FORTIFY_SOURCE
)
target_link_libraries(
        libexec-t1plus.so
        -pie
)

add_executable(
        libcmd-t1plus.so
        cmdexec.c
        socket.c
        atomicio.c
)
target_compile_definitions(
        libcmd-t1plus.so
        PRIVATE
        PACKAGE_NAME="${APPLICATION_ID}"
)
target_compile_options(
        libcmd-t1plus.so
        PRIVATE
        -fPIC
        # build without fortify, compatibility with SDK < 17!
        -U_FORTIFY_SOURCE
)
target_link_libraries(
        libcmd-t1plus.so
        -pie
)

add_library(
        appwrap
        SHARED
        t1p-wrap.c
        socket.c
        atomicio.c
)
target_compile_definitions(
        appwrap
        PRIVATE
        PACKAGE_NAME="${APPLICATION_ID}"
)
target_compile_options(
        appwrap
        PRIVATE
        # build without fortify, compatibility with SDK < 17!
        -U_FORTIFY_SOURCE
)
target_link_libraries(
        appwrap
)
