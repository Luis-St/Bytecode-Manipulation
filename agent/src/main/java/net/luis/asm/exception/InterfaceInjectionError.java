package net.luis.asm.exception;

import org.jetbrains.annotations.Nullable;

/**
 *
 * @author Luis-St
 *
 */

public class InterfaceInjectionError extends Error {
	
	public InterfaceInjectionError() {
		super();
	}

	public InterfaceInjectionError(@Nullable String message) {
		super(message);
	}

	public InterfaceInjectionError(@Nullable String message, @Nullable Throwable cause) {
		super(message, cause);
	}

	public InterfaceInjectionError(@Nullable Throwable cause) {
		super(cause);
	}
}
