package net.luis;

import net.luis.agent.annotation.implementation.InjectInterface;
import net.luis.agent.annotation.implementation.Inject;
import net.luis.agent.annotation.util.Target;
import net.luis.agent.util.TargetType;
import net.luis.utils.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

@InjectInterface(StringUtils.class)
public interface IStringUtils {
	
	@Inject(target = @Target(value = "String#contains(CharSequence)", type = TargetType.INVOKE, ordinal = 1))
	static void injectRemoveQuoted() {
		System.out.println("Injecting StringUtils#removeQuoted method");
	}
	
	@Inject(target = @Target(value = "List#isEmpty", type = TargetType.INVOKE))
	static @Nullable Boolean injectIsAfterAllOccurrence() {
		System.out.println("Injecting StringUtils#isAfterAllOccurrence method");
		return new Random().nextBoolean() ? true : null;
	}
}
