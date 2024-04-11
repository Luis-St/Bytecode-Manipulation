package net.luis.asm;

import org.jetbrains.annotations.Nullable;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 *
 * @author Luis-St
 *
 */

public class InterfaceClassTransformer implements ClassFileTransformer {
	
	@Override
	public byte @Nullable [] transform(ClassLoader loader, String name, Class<?> clazz, ProtectionDomain domain, byte[] buffer) throws IllegalClassFormatException {
		
		
		
		
		
		return null;
	}
}
