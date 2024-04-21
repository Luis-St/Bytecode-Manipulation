package net.luis;

import net.luis.annotation.InjectInterface;
import net.luis.utils.logging.LoggerConfiguration;

@InjectInterface(targets = LoggerConfiguration.class)
public interface MyInterface {}
