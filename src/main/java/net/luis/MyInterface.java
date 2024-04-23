package net.luis;

import net.luis.annotation.*;
import net.luis.utils.logging.LoggerConfiguration;
import net.luis.utils.logging.LoggingType;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configuration;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

@InjectInterface(targets = LoggerConfiguration.class)
public interface MyInterface {
	
	@Implemented
	@NotNull Configuration build();
}
