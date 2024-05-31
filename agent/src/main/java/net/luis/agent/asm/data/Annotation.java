package net.luis.agent.asm.data;

import net.luis.agent.AgentContext;
import net.luis.agent.asm.ASMUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

@SuppressWarnings("unchecked")
public class Annotation {
	
	private final Type type;
	private final boolean visible;
	private final Map<String, Object> values;
	
	private Annotation(@NotNull Type type, boolean visible, @NotNull Map<String, Object> values) {
		this.type = Objects.requireNonNull(type);
		this.visible = visible;
		this.values = Objects.requireNonNull(values);
	}
	
	public static @NotNull Annotation of(@NotNull Type type) {
		return new Annotation(type, true, new HashMap<>());
	}
	
	public static @NotNull Builder builder() {
		return new Builder();
	}
	
	public static @NotNull Builder builder(@NotNull Type type) {
		return new Builder(type);
	}
	
	public static @NotNull Builder builder(@NotNull Annotation annotation) {
		return new Builder(annotation);
	}
	
	//region Getters
	public @NotNull Type getType() {
		return this.type;
	}
	
	public boolean isVisible() {
		return this.visible;
	}
	
	public @NotNull Map<String, Object> getValues() {
		return this.values;
	}
	//endregion
	
	//region Functional getters
	public <X> @Nullable X get(@Nullable String key) {
		if (this.values.containsKey(key)) {
			return (X) this.values.get(key);
		}
		return null;
	}
	
	public <X> @Nullable X getDefault(@Nullable String key) {
		Class data = AgentContext.get().getClassData(this.type);
		List<Method> methods = data.getMethods(key);
		if (methods.size() != 1) {
			return null;
		}
		return (X) methods.getFirst().getAnnotationDefault().get();
	}
	
	public <X> @NotNull X getOrDefault(@Nullable String key) {
		return Objects.requireNonNull(this.values.containsKey(key) ? this.get(key) : this.getDefault(key));
	}
	
	public boolean is(@NotNull Type type) {
		return this.type.equals(type);
	}
	
	public boolean isAny(@NotNull Type... type) {
		return Arrays.stream(type).anyMatch(this::is);
	}
	//endregion
	
	//region Object overrides
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Annotation that)) return false;
		
		if (this.visible != that.visible) return false;
		if (!this.type.equals(that.type)) return false;
		return this.values.equals(that.values);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.type, this.visible, this.values);
	}
	
	@Override
	public String toString() {
		return "@" + ASMUtils.getSimpleName(this.type);
	}
	//endregion
	
	//region Builder
	public static class Builder {
		
		private final Map<String, Object> values = new HashMap<>();
		private Type type;
		private boolean visible;
		
		//region Constructors
		private Builder() {}
		
		private Builder(@NotNull Type type) {
			this.type = type;
		}
		
		private Builder(@NotNull Annotation annotation) {
			this.type = annotation.type;
			this.visible = annotation.visible;
			this.values.putAll(annotation.values);
		}
		//endregion
		
		public @NotNull Builder type(@NotNull Type type) {
			this.type = type;
			return this;
		}
		
		public @NotNull Builder visible(boolean visible) {
			this.visible = visible;
			return this;
		}
		
		//region Values
		public @NotNull Builder values(@NotNull Map<String, Object> values) {
			this.values.clear();
			this.values.putAll(values);
			return this;
		}
		
		public @NotNull Builder clearValues() {
			this.values.clear();
			return this;
		}
		
		public @NotNull Builder addValue(@NotNull String key, @Nullable Object value) {
			this.values.put(key, value);
			return this;
		}
		
		public @NotNull Builder removeValue(@NotNull String key) {
			this.values.remove(key);
			return this;
		}
		//endregion
		
		public @NotNull Annotation build() {
			return new Annotation(this.type, this.visible, this.values);
		}
	}
	//endregion
}
