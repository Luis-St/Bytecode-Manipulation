package net.luis.agent.asm.scope;

import net.luis.agent.asm.ASMUtils;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;

import java.util.Objects;
import java.util.Set;

public class ScopeBoundary {
	
	public static final ScopeBoundary ROOT;
	
	private Boundary start;
	private Boundary end;
	
	ScopeBoundary() {}
	
	//region Getters
	public boolean isRoot() {
		return this.start.line == 0 && this.end.line == Integer.MAX_VALUE;
	}
	
	public boolean isUnknown() {
		return this.start == null || this.end == null;
	}
	//endregion
	
	public void start(@NotNull Label label, @NotNull ScopeBoundary parent, @NotNull Set<ScopeBoundary> scopes) {
		int line = ASMUtils.getLine(label);
		if (line != -1) {
			this.start = Boundary.of(line);
		} else {
			line = parent.start.line;
			int generated = parent.end.generated + 1;
			if (scopes.isEmpty()) {
				this.start = new Boundary(line, generated);
			} else {
				scopes.stream().mapToInt(scope -> scope.end.generated).max().orElseThrow();
				
				
				
				
				
			}
			
			
			
		
		
		
		
		
		}
		
		
		
	}
	
	public boolean isInScope() {
		return false;
	}
	
	public void end(@NotNull Label label) {
		int line = ASMUtils.getLine(label);
	}
	
	static {
		ROOT = new ScopeBoundary();
		ROOT.start = Boundary.of(0);
		ROOT.end = Boundary.of(Integer.MAX_VALUE);
	}
	
	private static class Boundary {
		
		private final int line;
		private final int generated;
		
		Boundary() {
			this.line = 0;
			this.generated = 0;
		}
		
		Boundary(int line, int generated) {
			this.line = line;
			this.generated = generated;
		}
		
		public static @NotNull Boundary of(int line) {
			return new Boundary(line, 0);
		}
		
		//region Object overrides
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Boundary boundary)) return false;
			
			if (this.line != boundary.line) return false;
			return this.generated == boundary.generated;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.line, this.generated);
		}
		
		@Override
		public String toString() {
			return this.line + "." + this.generated;
		}
		//endregion
	}
}
