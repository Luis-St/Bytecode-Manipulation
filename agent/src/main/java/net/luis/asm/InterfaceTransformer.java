package net.luis.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 *
 * @author Luis
 *
 */

public class InterfaceTransformer implements ClassFileTransformer {
	
	@Override
	public byte[] transform(ClassLoader loader, String name, Class<?> clazz, ProtectionDomain domain, byte[] buffer) {
		
		ClassReader reader = new ClassReader(buffer);
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM8, classWriter) {
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				/*String[] newInterfaces = new String[interfaces.length + 1];
				System.arraycopy(interfaces, 0, newInterfaces, 0, interfaces.length);
				newInterfaces[interfaces.length] = "INumber";
				super.visit(version, access, name, signature, superName, newInterfaces);*/
				for (String anInterface : interfaces) {
					System.out.println("Interface: " + anInterface);
				}
				super.visit(version, access, name, signature, superName, interfaces);
			}
		};
		
		return buffer;
	}
}
