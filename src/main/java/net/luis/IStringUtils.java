package net.luis;

import net.luis.agent.annotation.implementation.InjectInterface;
import net.luis.agent.annotation.implementation.Injector;
import net.luis.utils.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

@InjectInterface(targets = StringUtils.class)
public interface IStringUtils {
	
	@Injector(target = "String#contains(CharSequence)", ordinal = 1)
	public static void injectRemoveQuoted() {
		System.out.println("Injecting StringUtils#removeQuoted method");
	}
	
	@Injector(target = "List#isEmpty")
	public static @Nullable Boolean injectIsAfterAllOccurrence() {
		System.out.println("Injecting StringUtils#isAfterAllOccurrence method");
		return new Random().nextBoolean() ? true : null;
	}
}
