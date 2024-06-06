package net.luis.agent.asm.scope;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Scope {
	
	private final Scope parent;
	private final ScopeBoundary boundary = new ScopeBoundary();
	private final Set<Integer> variables = new HashSet<>();
	private final List<Scope> scopes = new LinkedList<>();
	
	//region Constructors
	private Scope(@NotNull Scope parent) {
		this.parent = parent;
	}
	
	private Scope(@Nullable Scope parent, @NotNull Set<Integer> parameters) {
		this.parent = parent;
		this.variables.addAll(parameters);
	}
	//endregion
	
	public static @NotNull Scope root() {
		return new Scope(null, new HashSet<>());
	}
	
	public static @NotNull Scope root(int parameterCount) {
		return new Scope(null, IntStream.range(0, parameterCount).boxed().collect(Collectors.toSet()));
	}
	
	//region Getters
	public boolean isRoot() {
		return this.parent == null;
	}
	
	public Scope getParent() {
		return this.parent;
	}
	
	public @NotNull Set<Integer> getGlobalVariables() {
		Set<Integer> globals = new HashSet<>();
		
		Scope scope = this;
		while (scope != null) {
			globals.addAll(scope.variables);
			scope = scope.parent;
		}
		return globals;
	}
	
	public @NotNull Set<Integer> getScopeVariables() {
		return this.variables;
	}
	
	public @NotNull List<Scope> getScopes() {
		return this.scopes;
	}
	
	public Scope getScope(int index) {
		return this.scopes.get(index);
	}
	//endregion
	
	public @NotNull Scope openScope(@NotNull Label label) {
		Scope scope = new Scope(this);
		scope.boundary.start(label);
		this.scopes.add(scope);
		return scope;
	}
	
	public void visitVariable(int variable) {
		this.variables.add(variable);
	}
	
	public @NotNull Scope closeScope(@NotNull Label label) {
		this.boundary.end(label);
		return this.parent;
	}
}
