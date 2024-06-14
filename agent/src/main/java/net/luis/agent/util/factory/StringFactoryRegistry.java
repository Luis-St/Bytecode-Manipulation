package net.luis.agent.util.factory;

import net.luis.utils.io.reader.ScopedStringReader;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import static net.luis.agent.util.factory.DefaultStringFactories.*;

/**
 *
 * @author Luis-St
 *
 */

public class StringFactoryRegistry implements StringFactory {
	
	public static final StringFactoryRegistry INSTANCE = new StringFactoryRegistry();
	
	private final List<RegistryEntry> entries = new ArrayList<>();
	
	private StringFactoryRegistry() {
		this.registerFactory(Pattern.compile("^(boolean|java\\.lang\\.Boolean)$"), createSimple(ScopedStringReader::readBoolean));
		this.registerFactory(Pattern.compile("^(byte|java\\.lang\\.Byte)$"), createSimple(ScopedStringReader::readByte));
		this.registerFactory(Pattern.compile("^(char|java\\.lang\\.Character)$"), createSimple(ScopedStringReader::read));
		this.registerFactory(Pattern.compile("^(short|java\\.lang\\.Short)$"), createSimple(ScopedStringReader::readShort));
		this.registerFactory(Pattern.compile("^(int|java\\.lang\\.Integer)$"), createSimple(ScopedStringReader::readInt));
		this.registerFactory(Pattern.compile("^(long|java\\.lang\\.Long)$"), createSimple(ScopedStringReader::readLong));
		this.registerFactory(Pattern.compile("^(float|java\\.lang\\.Float)$"), createSimple(ScopedStringReader::readFloat));
		this.registerFactory(Pattern.compile("^(double|java\\.lang\\.Double)$"), createSimple(ScopedStringReader::readDouble));
		this.registerFactory(Pattern.compile("^java\\.lang\\.String$"), createSimple(ScopedStringReader::readString));
		this.registerFactory(Pattern.compile("^[^\\[]*\\[[^\\[]*$"), DefaultStringFactories::createArray);
	}
	
	@Override
	public @NotNull Object create(@NotNull String type, @NotNull ScopedStringReader reader) {
		for (RegistryEntry entry : this.entries) {
			if (entry.matches(type)) {
				return entry.factory().apply(type, reader);
			}
		}
		throw new IllegalArgumentException("No factory for type '" + type + "' in string factory registry found");
	}
	
	public void registerFactory(@NotNull String type, @NotNull BiFunction<String, ScopedStringReader, ?> factory) {
		this.entries.add(new StringRegistryEntry(type, factory));
	}
	
	public void registerFactory(@NotNull Pattern pattern, @NotNull BiFunction<String, ScopedStringReader, ?> factory) {
		this.entries.add(new PatternRegistryEntry(pattern, factory));
	}
	
	//region Internal
	private interface RegistryEntry {
		
		boolean matches(@NotNull String type);
		
		@NotNull BiFunction<String, ScopedStringReader, ?> factory();
	}
	
	private record StringRegistryEntry(@NotNull String type, @NotNull BiFunction<String, ScopedStringReader, ?> factory) implements RegistryEntry {
		
		@Override
		public boolean matches(@NotNull String type) {
			return this.type.equals(type);
		}
	}
	
	private record PatternRegistryEntry(@NotNull Pattern pattern, @NotNull BiFunction<String, ScopedStringReader, ?> factory) implements RegistryEntry {
		
		@Override
		public boolean matches(@NotNull String type) {
			return this.pattern.matcher(type).matches();
		}
	}
	//endregion
}
