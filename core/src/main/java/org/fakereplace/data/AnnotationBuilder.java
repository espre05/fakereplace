/*
 * Copyright 2016, Stuart Douglas, and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.fakereplace.data;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.ByteMemberValue;
import javassist.bytecode.annotation.CharMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.DoubleMemberValue;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.FloatMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.LongMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.ShortMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

/**
 * Created a Javassist Annotation from a java one
 *
 * @author stuart
 */
public class AnnotationBuilder {

    public static javassist.bytecode.annotation.Annotation createJavassistAnnotation(java.lang.annotation.Annotation annotation, ConstPool cp) {
        try {
            javassist.bytecode.annotation.Annotation a = new Annotation(annotation.annotationType().getName(), cp);
            for (Method m : annotation.annotationType().getDeclaredMethods()) {
                Object val = m.invoke(annotation);
                a.addMemberValue(m.getName(), createMemberValue(m.getReturnType(), val, cp));
            }
            return a;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static MemberValue createMemberValue(Class<?> type, Object val, ConstPool cp) {
        if (type == int.class) {
            return new IntegerMemberValue(cp, (Integer) val);
        } else if (type == short.class) {
            return new ShortMemberValue((Short) val, cp);
        } else if (type == long.class) {
            return new LongMemberValue((Long) val, cp);
        } else if (type == byte.class) {
            return new ByteMemberValue((Byte) val, cp);
        } else if (type == float.class) {
            return new FloatMemberValue((Float) val, cp);
        } else if (type == double.class) {
            return new DoubleMemberValue((Double) val, cp);
        } else if (type == char.class) {
            return new CharMemberValue((Character) val, cp);
        } else if (type == boolean.class) {
            return new BooleanMemberValue((Boolean) val, cp);
        } else if (type == String.class) {
            return new StringMemberValue((String) val, cp);
        } else if (type == Class.class) {
            return new ClassMemberValue(((Class<?>) val).getName(), cp);
        } else if (type.isEnum()) {
            EnumMemberValue e = new EnumMemberValue(cp);
            e.setType(type.getName());
            e.setValue(((Enum<?>) val).name());
            return e;
        } else if (type.isAnnotation()) {
            return new AnnotationMemberValue(createJavassistAnnotation((java.lang.annotation.Annotation) val, cp), cp);
        } else if (type.isArray()) {
            Class<?> arrayType = type.getComponentType();
            int length = Array.getLength(val);
            MemberValue arrayval = createEmptyMemberValue(arrayType, cp);
            ArrayMemberValue ret = new ArrayMemberValue(arrayval, cp);
            MemberValue[] vals = new MemberValue[length];
            for (int i = 0; i < length; ++i) {
                vals[i] = createMemberValue(arrayType, Array.get(val, i), cp);
            }
            ret.setValue(vals);
            return ret;
        }
        throw new RuntimeException("Invalid array type " + type + " value: " + val);

    }

    private static MemberValue createEmptyMemberValue(Class<?> type, ConstPool cp) {
        if (type == int.class) {
            return new IntegerMemberValue(cp);
        } else if (type == short.class) {
            return new ShortMemberValue(cp);
        } else if (type == long.class) {
            return new LongMemberValue(cp);
        } else if (type == byte.class) {
            return new ByteMemberValue(cp);
        } else if (type == float.class) {
            return new FloatMemberValue(cp);
        } else if (type == double.class) {
            return new DoubleMemberValue(cp);
        } else if (type == char.class) {
            return new CharMemberValue(cp);
        } else if (type == boolean.class) {
            return new BooleanMemberValue(cp);
        } else if (type == String.class) {
            return new StringMemberValue(cp);
        } else if (type == Class.class) {
            return new ClassMemberValue(cp);
        } else if (type.isEnum()) {
            EnumMemberValue e = new EnumMemberValue(cp);
            e.setType(type.getName());
            return e;
        } else if (type.isAnnotation()) {
            AnnotationMemberValue a = new AnnotationMemberValue(cp);
            return a;
        } else if (type.isArray()) {
            Class<?> arrayType = type.getComponentType();
            MemberValue arrayval = createEmptyMemberValue(arrayType, cp);
            ArrayMemberValue ret = new ArrayMemberValue(arrayval, cp);
            return ret;
        }
        throw new RuntimeException("Invalid array type " + type + " with no value ");
    }
}
