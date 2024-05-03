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
	
	@Implemented // Ignored, because implemented in the target
	@NotNull Configuration build();
	
	@Accessor // Makes the logger field accessible
	@NotNull List<String> getLoggers();
	
	@Accessor(target = "allowedTypes") // Makes the allowedTypes field accessible (target required, because method name does not match)
	@NotNull Set<LoggingType> getTypes();
	
	@Assignor // Makes the logger field assignable
	void setLoggers(@NotNull List<String> loggers);
	
	@Assignor(target = "allowedTypes") // Makes the allowedTypes field assignable (target required, because method name does not match)
	void setTypes(@NotNull Set<LoggingType> types);
	
	@Invoker // Invokes private method
	@NotNull String invokeGetPattern(@NotNull LoggingType type, @NotNull Level level);
	
	@Invoker(target = "getPattern(LoggingType, Level)") // Invokes private method (target required, because method name does not match)
	@NotNull String getLoggingPattern(@NotNull LoggingType type, @NotNull Level level);
}
