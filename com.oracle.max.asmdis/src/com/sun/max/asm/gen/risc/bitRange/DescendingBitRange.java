/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.asm.gen.risc.bitRange;

/**
 * A bit range that has its most significant bit on the left and its least significant bit on the right.
 */
public class DescendingBitRange extends SimpleBitRange {

    public DescendingBitRange(int firstIndex, int lastIndex) {
        super(firstIndex, lastIndex);
        if (firstIndex < lastIndex) {
            throw new IllegalArgumentException("bit ranges are specified from left to right, and descending notation starts at 31 and goes down to 0");
        }
    }

    @Override
    public DescendingBitRange move(boolean left, int bits) {
        if (left) {
            return new DescendingBitRange(firstBitIndex + bits, lastBitIndex + bits);
        }
        return new DescendingBitRange(firstBitIndex - bits, lastBitIndex - bits);
    }

    @Override
    public int numberOfLessSignificantBits() {
        return lastBitIndex;
    }

    @Override
    public int width() {
        return (firstBitIndex - lastBitIndex) + 1;
    }
}
