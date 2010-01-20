/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.cps.cir.transform;

import java.util.*;

import com.sun.max.vm.cps.cir.*;

/**
 * Finds out in which block each CIR call is nested and remembers this in a dictionary for later queries.
 * 
 * @author Bernd Mathiske
 */
public class CirScopedBlockUpdating extends CirBlockScopedTraversal {

    public CirScopedBlockUpdating(CirNode graph) {
        super(graph);
    }

    @Override
    public void visitBlock(CirBlock block, CirBlock scope) {
        block.reset();
        super.visitBlock(block, scope);
    }

    final LinkedList<CirCall> blockCalls = new LinkedList<CirCall>();

    public LinkedList<CirCall> blockCalls() {
        return blockCalls;
    }

    final Map<CirCall, CirBlock> callToScope = new IdentityHashMap<CirCall, CirBlock>();

    /**
     * @return the innermost block that contains the given call
     */
    public CirBlock scope(CirCall call) {
        assert call.javaFrameDescriptor() != null || call.procedure() instanceof CirBlock;
        return callToScope.get(call);
    }

    @Override
    public void visitCall(CirCall call, CirBlock scope) {
        if (call.javaFrameDescriptor() != null) {
            assert !CirBlock.class.isInstance(call.procedure());
            callToScope.put(call, scope);
        } else if (call.procedure() instanceof CirBlock) {
            callToScope.put(call, scope);
            blockCalls.add(call);
        }
        super.visitCall(call, scope);
    }

    @Override
    public void run() {
        super.run();
        for (CirCall call : blockCalls) {
            final CirBlock block = (CirBlock) call.procedure();
            block.addCall(call);
        }
    }

}