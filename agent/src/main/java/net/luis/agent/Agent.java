package net.luis.agent;

import net.luis.asm.InterfaceTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.lang.instrument.*;
import java.security.ProtectionDomain;

/**
 *
 * @author Luis
 *
 */

public class Agent {
	public static void premain(String agentArgs, Instrumentation inst) throws IOException {
		ClassDefinition[] classDefs = new ClassDefinition[] {new ClassDefinition(Number.class, getModifiedBytecode())};
		try {
			inst.redefineClasses(classDefs);
		} catch (ClassNotFoundException | UnmodifiableClassException e) {
			e.printStackTrace();
		}
	}
	
	private static byte[] getModifiedBytecode() throws IOException {
		// Load the bytecode of the java.lang.Number class using a ClassReader
		ClassReader cr = new ClassReader(Number.class.getName());
		// Modify the bytecode using the NumberModifier ClassVisitor
		ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
		cr.accept(new NumberModifier(cw), ClassReader.EXPAND_FRAMES);
		// Get the modified bytecode as a byte array
		return cw.toByteArray();
	}
	
	private static class NumberModifier extends ClassVisitor {
		public NumberModifier(ClassVisitor cv) {
			super(Opcodes.ASM9, cv);
		}
		
		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			// Add the INumber interface to the list of interfaces implemented by java.lang.Number
			String[] newInterfaces = new String[interfaces.length + 1];
			System.arraycopy(interfaces, 0, newInterfaces, 0, interfaces.length);
			newInterfaces[interfaces.length] = "INumber";
			super.visit(version, access, name, signature, superName, interfaces);
		}
	}
}