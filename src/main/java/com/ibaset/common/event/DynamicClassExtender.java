/**
 * Proprietary and Confidential
 * Copyright 1995-2011 iBASEt, Inc.
 * Unpublished-rights reserved under the Copyright Laws of the United States
 * US Government Procurements:
 * Commercial Software licensed with Restricted Rights.
 * Use, reproduction, or disclosure is subject to restrictions set forth in
 * license agreement and purchase contract.
 * iBASEt, Inc. 27442 Portola Parkway, Suite 300, Foothill Ranch, CA 92610
 *
 * Solumina software may be subject to United States Dept of Commerce Export Controls.
 * Contact iBASEt for specific Expert Control Classification information.
 */
package com.ibaset.common.event;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.ClassWriter;
import org.springframework.asm.FieldVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;

import com.ibaset.common.ImplementationOf;

import net.sf.cglib.asm.Constants;
import net.sf.cglib.core.TypeUtils;

public class DynamicClassExtender 
{
	private static final Logger logger = LoggerFactory.getLogger(DynamicClassExtender.class);
	static ExtenderClassLoader ecl = new ExtenderClassLoader(getClassLoader(DynamicClassExtender.class));
	static class ExtensionInfo
	{
		String className;
		Class<?> baseClass;
		Class<?> extendedInterface;
		Class<?> extensionClass;
		
	}
	static class MethodScanner extends MethodVisitor
	{
		//all names are internal names
		private final String superName;
		private final String extensionInterface;
		private final String extensionClassName;
		private final String className;
		private final String implClassName;
		
		public MethodScanner(MethodVisitor wrap, ExtensionInfo info) 
		{
			super(Opcodes.ASM4, wrap);
			this.className = TypeUtils.parseType(info.className).getInternalName();
			this.superName = Type.getType(info.baseClass).getInternalName();
			this.extensionInterface = Type.getType(info.extendedInterface).getInternalName();
			this.extensionClassName = Type.getType(info.extensionClass).getInternalName();
			this.implClassName = Type.getType(ImplementationOf.class).getInternalName();
		}

		@Override
		public void visitTypeInsn(int opcode, String desc) {
			if(opcode == Constants.CHECKCAST && desc.equals(extensionInterface))
			{
				//remove checkcast interface
			}
			else
			{
				if(desc.equals(extensionClassName)) 
				{
					desc = className;
				}
				super.visitTypeInsn(opcode, desc);
			}
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String desc) 
		{
			if(opcode == Constants.GETFIELD && name.equals("Super"))
			{
				//remove references to Super
			}
			else
			{
				//replace extension name with the generated name
				if(owner.equals(extensionClassName)) 
				{
					owner = className;
				}
				super.visitFieldInsn(opcode, owner, name, desc);
			}
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc) 
		{
			if(owner.equals(implClassName))
			{
				//replace super class name 
				owner = superName;
			}
			else if(opcode == Constants.INVOKEINTERFACE && owner.equals(extensionInterface))
			{
				//call super class instead of interface method
				opcode = Constants.INVOKESPECIAL;
				owner = superName;
			}
			else if(owner.equals(extensionClassName)) 
			{
				//replace extension name with the generated name
				owner = className;
			}
			super.visitMethodInsn(opcode, owner, name, desc);
		}

	}
	static class ClassExtender extends ClassVisitor
	{
		private final Type superType;
		private final ExtensionInfo extensionInfo;
		private final String className;
		
		public ClassExtender(ClassVisitor v, ExtensionInfo info) 
		{
			super(Opcodes.ASM4, v);
			this.extensionInfo = info;
			this.superType = Type.getType(info.baseClass);
			this.className = TypeUtils.parseType(info.className).getInternalName();
		}
		
		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) 
		{
			//replace name, access and super type
			super.visit(version, Constants.ACC_PUBLIC, className, (String)null, this.superType.getInternalName(), interfaces);
		}
		
		@Override
		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) 
		{
			//skip Super field
			if(name.equals("Super"))
			{
				return null;
			}
			return super.visitField(access, name, desc, signature, value);
		}
		
		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) 
		{
			return new MethodScanner(
					super.visitMethod(access, name, desc, signature, exceptions), 
					extensionInfo);
		}
	}
    public static ClassLoader getClassLoader(Class<?> c) {
        ClassLoader t = c.getClassLoader();
        if (t == null) {
            t = Thread.currentThread().getContextClassLoader();
        }
        if (t == null) {
            throw new IllegalStateException("Cannot determine classloader");
        }
        return t;
    }
    

	static class ExtenderClassLoader extends ClassLoader
	{
		public ExtenderClassLoader(ClassLoader parent) {
			super(parent);
		}
		public Class<?> defineClass(String name, byte[] b)
		{
			return defineClass(name, b, 0, b.length);
		}
	}
	
	public  static Class<?> extend(Class<?> baseClass, Class<?> extendedInterface, Class<?> extension) throws IOException
	{
		Class<?> extensionSuper = extension.getSuperclass(); 
		if(!ImplementationOf.class.equals(extensionSuper))
		{
			throw new IllegalArgumentException("Extension class "+extension.getName()+ " must extend "+ImplementationOf.class.getName());
		}
		ClassReader ext = getClassReader(extension);
		ClassWriter result = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		//ClassWriter result = new ClassWriter(true, false);
		String newClassName = extension.getName()+"$$"+baseClass.getSimpleName();
		ExtensionInfo info = new ExtensionInfo();
		info.className = newClassName;
		info.baseClass = baseClass;
		info.extendedInterface = extendedInterface;
		info.extensionClass = extension;
		ClassExtender extender = new ClassExtender(result, info);
		//ext.accept(extender, false);
		ext.accept(extender, ClassReader.SKIP_FRAMES);
		result.visitEnd();
		byte[] code = result.toByteArray();
		if (logger.isDebugEnabled()) {
			logger.debug("Dynamically defining new extended class for extension: " + extension.getName()
					+ " and extended interface: " + extendedInterface.getName());
		}
		//ExtenderClassLoader ecl = new ExtenderClassLoader(getClassLoader(extension));
		Class<?> clazz =  ecl.defineClass(newClassName, code);
		
		return clazz;
	}
	
	private static ClassReader getClassReader(Class<?> clazz) throws IOException
	{
		final String name = clazz.getName().replace('.', '/')+".class";
		InputStream is = getClassLoader(clazz).getResourceAsStream(name);
		if(is == null) throw new FileNotFoundException(name);
		return new ClassReader(is);
	}
}

