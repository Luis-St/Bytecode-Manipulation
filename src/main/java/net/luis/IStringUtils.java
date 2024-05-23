package net.luis;

import net.luis.agent.annotation.implementation.InjectInterface;
import net.luis.agent.annotation.implementation.Injector;
import net.luis.utils.lang.StringUtils;

@InjectInterface(targets = StringUtils.class)
public interface IStringUtils {
	
	@Injector(target = "String#contains(CharSequence)", ordinal = 1)
	public static void injectRemoveQuoted() {
		System.out.println("Injecting StringUtils#removeQuoted method");
	}
}
