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
package com.sun.max.vm.classfile.constant;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.ConstantPool.*;
import com.sun.max.vm.type.*;

/**
 * #4.4.2.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public interface FieldRefConstant extends PoolConstant<FieldRefConstant>, MemberRefConstant<FieldRefConstant> {

    public static interface FieldRefKey extends PoolConstantKey<FieldRefConstant> {

        TypeDescriptor holder();

        Utf8Constant name();

        TypeDescriptor type();

        public static final class Util {

            private Util() {
            }

            public static boolean equals(FieldRefKey key, Object other) {
                if (other instanceof FieldRefKey) {
                    final FieldRefKey otherKey = (FieldRefKey) other;
                    return key.holder().equals(otherKey.holder()) && key.name().equals(otherKey.name()) && key.type().equals(otherKey.type());
                }
                return false;
            }
            public static int hashCode(FieldRefKey key) {
                return key.holder().hashCode() ^ key.name().hashCode() ^ key.type().hashCode();
            }
        }
    }

    TypeDescriptor type(ConstantPool pool);

    FieldActor resolve(ConstantPool pool, int index);

    FieldRefKey key(final ConstantPool pool);

    public static final class Resolved extends AbstractPoolConstant<FieldRefConstant> implements FieldRefConstant, FieldRefKey {

        @INSPECTED
        private final FieldActor fieldActor;

        public FieldActor fieldActor() {
            return fieldActor;
        }

        public Resolved(FieldActor fieldActor) {
            this.fieldActor = fieldActor;
        }

        @Override
        public Tag tag() {
            return Tag.FIELD_REF;
        }

        public boolean isResolvableWithoutClassLoading(ConstantPool pool) {
            return true;
        }

        public boolean isResolved() {
            return true;
        }

        public FieldActor resolve(ConstantPool pool, int index) {
            return fieldActor;
        }

        public TypeDescriptor holder() {
            return fieldActor.holder().typeDescriptor;
        }

        public Utf8Constant name() {
            return fieldActor.name;
        }

        public TypeDescriptor type() {
            return fieldActor.descriptor();
        }

        public Utf8Constant name(ConstantPool pool) {
            return name();
        }

        public TypeDescriptor holder(ConstantPool pool) {
            return holder();
        }

        public Descriptor descriptor(ConstantPool pool) {
            return type();
        }

        public TypeDescriptor type(ConstantPool pool) {
            return type();
        }

        @Override
        public boolean equals(Object object) {
            return FieldRefKey.Util.equals(this, object);
        }

        @Override
        public int hashCode() {
            return FieldRefKey.Util.hashCode(this);
        }

        @Override
        public FieldRefKey key(ConstantPool pool) {
            return this;
        }

        public String valueString(ConstantPool pool) {
            return fieldActor.format("%H.%n:%t");
        }
    }

    static final class Unresolved extends UnresolvedRef<FieldRefConstant> implements FieldRefConstant, FieldRefKey {

        Unresolved(ClassActor holder, Utf8Constant name, Descriptor descriptor) {
            super(holder, name, descriptor);
        }

        @Override
        public Tag tag() {
            return Tag.FIELD_REF;
        }

        static FieldActor resolve(ConstantPool pool, int index, ClassActor holder, Utf8Constant name, TypeDescriptor type) {
            final FieldActor fieldActor = holder.findFieldActor(name, type);
            if (fieldActor != null) {
                fieldActor.checkAccessBy(pool.holder());
                pool.updateAt(index, new Resolved(fieldActor));
                return fieldActor;
            }
            final String errorMessage = type + " " + holder.javaSignature(true) + "." + name;
            if (MaxineVM.isPrototyping()) {
                final Class<?> javaClass = holder.toJava();
                final Class fieldType = type.resolveType(javaClass.getClassLoader());
                final Field field = Classes.resolveField(javaClass, fieldType, name.string);
                if (MaxineVM.isPrototypeOnly(field)) {
                    throw new PrototypeOnlyFieldError(errorMessage);
                }
            }

            throw new NoSuchFieldError(errorMessage);
        }

        public FieldActor resolve(ConstantPool pool, int index) {
            return resolve(pool, index, holder, name, type());
        }

        @Override
        public FieldRefKey key(ConstantPool pool) {
            return this;
        }

        @Override
        public boolean equals(Object object) {
            return FieldRefKey.Util.equals(this, object);
        }

        @Override
        public int hashCode() {
            return FieldRefKey.Util.hashCode(this);
        }

        @Override
        boolean isFieldConstant() {
            return true;
        }
    }

    static final class UnresolvedIndices extends UnresolvedRefIndices<FieldRefConstant> implements FieldRefConstant {

        UnresolvedIndices(int classIndex, int nameAndTypeIndex, Tag[] tags) {
            super(classIndex, nameAndTypeIndex, tags);
        }

        @Override
        public Tag tag() {
            return Tag.FIELD_REF;
        }

        @Override
        public FieldRefKey key(final ConstantPool pool) {
            class Key extends RefKey implements FieldRefKey {
                Key() {
                    super(pool, UnresolvedIndices.this);
                }

                public final TypeDescriptor type() {
                    return UnresolvedIndices.this.type(pool);
                }

                @Override
                public boolean equals(Object object) {
                    return FieldRefKey.Util.equals(this, object);
                }

                @Override
                public int hashCode() {
                    return FieldRefKey.Util.hashCode(this);
                }
            }
            return new Key();
        }

        public FieldActor resolve(ConstantPool pool, int index) {
            final ClassActor classActor = pool.classAt(classIndex).resolve(pool, classIndex);
            return Unresolved.resolve(pool, index, classActor, name(pool), type(pool));
        }

        @Override
        boolean isFieldConstant() {
            return true;
        }
    }
}
