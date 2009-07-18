#!/bin/bash
#
# ----------------------------------------------------------------------------------------------------
# Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
#
# Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
# that is described in this document. In particular, and without limitation, these intellectual property
# rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
# more additional patents or pending patent applications in the U.S. and in other countries.
#
# U.S. Government Rights - Commercial software. Government users are subject to the Sun
# Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
# supplements.
#
# Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
# registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
# are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
# U.S. and other countries.
#
# UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
# Company, Ltd.
# ----------------------------------------------------------------------------------------------------
#
# This script copies libjava.jnilib from the JDK installation into the directory
# in which the Maxine's version of libjvm.dylib will reside. The copy is modified
# through use of install_name_tool(1) to change the hard-wired dependency to the
# JVM shared library from the libclient.dylib found in the JDK installation to
# the libjvm.dylib built as part of Maxine. Without this work-around, the functions
# in libjava.jnilib that call any of the JVM_* functions will pick up those from
# HotSpot instead of those from Maxine. It should go without saying that this 
# is not the desired behavior.
#
# The underlying issue is the support on Darwin for two-level namespaces in
# shared libraries (http://developer.apple.com/documentation/Porting/Conceptual/PortingUnix/compiling/chapter_4_section_7.html).
# While this most likely improves the startup time of HotSpot, it complicates
# the task of deploying a drop-in replacement for HotSpot. One other workaround
# is to use the DYLD_FORCE_FLAT_NAMESPACE environment variable (see the dyld(1)
# man page). However, environment variables are not propagated as expected through
# calls to dlopen. From the dlopen(3) man page:
#
#   Note: If the main executable is a set[ug]id binary, then all environment variables are ignored..
#
# For the inspector to be able to launch and control another process, the 'java'
# executable used to run the inspector must be setgid. Then, even if all
# the code is written to manually propagate/set DYLD_FORCE_FLAT_NAMESPACE in the
# various contexts in which the Maxine VM can be launched, the flat namespace
# linkage causes intolerable delays in the inspector. Most noticeable is the
# slow down of single-stepping to about 4 seconds per single step.
#
# Author: Doug Simon

# Sanity: exit immediately if a simple command exits with a non-zero status or if
# an unset variable is used when performing expansion
set -e
set -u

test $# -eq 2 || {
    echo "Usage: $0 <directory containing libjvm.dylib> <jdk-home>"
    exit 1
}

dir=$1
JAVA_HOME=$2

pushd $dir >/dev/null

# Make $dir absolute
dir=$(/bin/pwd)

new_jvmlib=$dir/libjvm.dylib
test -f $new_jvmlib || { echo "No libjvm.dylib in $dir"; exit 1; }

# Copy libjava from JDK installation
src=$JAVA_HOME/../Libraries/libjava.jnilib
test -f $src || { echo "Missing $src"; exit 1; }
cp -f $src .

lib=$dir/libjava.jnilib
old_jvmlib=$(otool -l $lib | grep libclient.dylib | awk '{print $2}')
test -n "$old_jvmlib" || {
    echo "Could not find line containing 'libclient.dylib' in $lib"
    exit 1
}

install_name_tool -change $old_jvmlib $new_jvmlib -id $lib $lib
echo "Copied $src to $lib and re-bound it from $old_jvmlib to $new_jvmlib"