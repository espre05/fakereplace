package org.fakereplace.manip;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

import org.fakereplace.boot.Constants;
import org.fakereplace.boot.Enviroment;
import org.fakereplace.boot.Logger;
import org.fakereplace.util.JumpMarker;
import org.fakereplace.util.JumpUtils;

/**
 * manipulator that replaces Method.invokewith the following:
 * <p>
 * <code>
 *  if(ConstructorReflection.fakeCallRequired)
 *     MethodReflection.invoke
 *  else
 *     method.invoke
 * </code>
 * 
 * @author stuart
 * 
 */
public class ConstructorReflectionManipulator implements ClassManipulator
{

   public static final String METHOD_NAME = "newInstance";
   public static final String REPLACED_METHOD_DESCRIPTOR = "(Ljava/lang/reflect/Constructor;[Ljava/lang/Object;)Ljava/lang/Object;";
   public static final String METHOD_DESCRIPTOR = "([Ljava/lang/Object;)Ljava/lang/Object;";

   public void clearRewrites(String className, ClassLoader loader)
   {

   }

   public void transformClass(ClassFile file, ClassLoader loader, Enviroment environment)
   {
      Set<Integer> methodCallLocations = new HashSet<Integer>();
      Integer newCallLocation = null;
      Integer constructorReflectionLocation = null;
      Integer fakeCallRequiredLocation = null;
      // first we need to scan the constant pool looking for
      // CONSTANT_method_info_ref structures
      ConstPool pool = file.getConstPool();
      for (int i = 1; i < pool.getSize(); ++i)
      {
         // we have a method call
         if (pool.getTag(i) == ConstPool.CONST_Methodref)
         {
            String className = pool.getMethodrefClassName(i);
            String methodName = pool.getMethodrefName(i);

            if (className.equals(Constants.CONSTRUCTOR_NAME))
            {
               if (methodName.equals(METHOD_NAME))
               {
                  // store the location in the const pool of the method ref
                  methodCallLocations.add(i);
                  // we have found a method call

                  // if we have not already stored a reference to our new
                  // method in the const pool
                  if (newCallLocation == null)
                  {
                     constructorReflectionLocation = pool.addClassInfo("org.fakereplace.reflection.ConstructorReflection");
                     int nt = pool.addNameAndTypeInfo("fakeCallRequired", "(Ljava/lang/reflect/Constructor;)Z");
                     fakeCallRequiredLocation = pool.addMethodrefInfo(constructorReflectionLocation, nt);
                     newCallLocation = pool.addNameAndTypeInfo(METHOD_NAME, REPLACED_METHOD_DESCRIPTOR);
                  }
               }
            }
         }
      }

      // this means we found an instance of the call, now we have to iterate
      // through the methods and replace instances of the call
      if (newCallLocation != null)
      {
         List<MethodInfo> methods = file.getMethods();
         for (MethodInfo m : methods)
         {
            try
            {
               // ignore abstract methods
               if (m.getCodeAttribute() == null)
               {
                  continue;
               }
               CodeIterator it = m.getCodeAttribute().iterator();
               while (it.hasNext())
               {
                  // loop through the bytecode
                  int index = it.next();
                  int op = it.byteAt(index);
                  // if the bytecode is a method invocation
                  if (op == CodeIterator.INVOKEVIRTUAL)
                  {
                     int val = it.s16bitAt(index + 1);
                     // if the method call is one of the methods we are
                     // replacing
                     if (methodCallLocations.contains(val))
                     {
                        Bytecode b = new Bytecode(file.getConstPool());
                        // our stack looks like Constructor,params
                        // we need Constructor, params, Constructor
                        b.add(Opcode.SWAP);
                        b.add(Opcode.DUP_X1);
                        b.addInvokestatic(constructorReflectionLocation, "fakeCallRequired", "(Ljava/lang/reflect/Constructor;)Z");
                        b.add(Opcode.IFEQ);
                        JumpMarker performRealCall = JumpUtils.addJumpInstruction(b);
                        // now perform the fake call
                        b.addInvokestatic(constructorReflectionLocation, METHOD_NAME, REPLACED_METHOD_DESCRIPTOR);
                        b.add(Opcode.GOTO);
                        JumpMarker finish = JumpUtils.addJumpInstruction(b);
                        performRealCall.mark();
                        b.addInvokevirtual(Constants.CONSTRUCTOR_NAME, METHOD_NAME, METHOD_DESCRIPTOR);
                        finish.mark();
                        it.writeByte(CodeIterator.NOP, index);
                        it.writeByte(CodeIterator.NOP, index + 1);
                        it.writeByte(CodeIterator.NOP, index + 2);
                        it.insert(b.get());
                     }
                  }

               }
               m.getCodeAttribute().computeMaxStack();
            }
            catch (Exception e)
            {
               Logger.log(this, "Bad byte code transforming " + file.getName());
               e.printStackTrace();
            }
         }
      }
   }

}