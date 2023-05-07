package net.luis.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

/**
 *
 * @author Luis
 *
 */

public class Agent {
	
	public static void premain(String agentArgs, Instrumentation inst) throws UnmodifiableClassException {
		System.out.println("Agent loaded!");
		inst.addTransformer(new NumberTransformer());
		if (inst.isModifiableClass(Number.class)) {
			inst.retransformClasses(Number.class);
			System.out.println("Number is modifiable!");
		} else {
			System.out.println("Number is not modifiable!");
		}
	}
	
	private static class NumberTransformer implements ClassFileTransformer {
		
		@Override
		public byte[] transform(ClassLoader loader, String name, Class<?> clazz, ProtectionDomain protection, byte[] buffer) {
			System.out.println("Try transforming: " + name);
			if ("java/lang/Number".equals(name)) {
				ClassReader cr = new ClassReader(buffer);
				ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
				ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
					@Override
					public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
						String[] newInterfaces = new String[interfaces.length + 1];
						System.arraycopy(interfaces, 0, newInterfaces, 0, interfaces.length);
						newInterfaces[interfaces.length] = "net/luis/override/INumber";
						System.out.println("Interfaces: " + interfaces.length + " -> " + newInterfaces.length);
						super.visit(version, access, name, signature, superName, newInterfaces);
					}
				};
				cr.accept(cv, ClassReader.EXPAND_FRAMES);
				System.out.println("Transformed: " + name);
				return cw.toByteArray();
			} else {
				return null;
			}
		}
	}
}