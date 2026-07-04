/*
 * System Shell Box
 * Copyright (C) 2025-2026 kgultrt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.manager.nativelayer;

public class XmpPlayer {
    static {
        System.loadLibrary("xmp_jni");
    }

    // 加载一个模块文件，返回是否成功
    public native boolean nativeInit(String filePath);

    // 填充音频缓冲区，返回播放状态（0继续，-XMP_END表示结束等）
    public native int nativeFillBuffer(short[] buffer, int size);

    // 释放资源
    public native void nativeRelease();
}