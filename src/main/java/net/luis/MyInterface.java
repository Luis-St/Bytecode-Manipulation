package net.luis;

import net.luis.annotation.InterfaceInjection;
import net.luis.utils.logging.LoggerConfiguration;

@InterfaceInjection(targets = LoggerConfiguration.class)
public interface MyInterface {
}
