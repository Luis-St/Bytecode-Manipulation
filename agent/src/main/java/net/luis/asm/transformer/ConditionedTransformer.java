package net.luis.asm.transformer;

import java.util.function.Supplier;

/**
 *
 * @author Luis-St
 *
 */

public interface ConditionedTransformer<T> extends Supplier<T> {
	
	@Override
	T get();
}
