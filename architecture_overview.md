# Java Bytecode Manipulation Project - Architecture Overview

## Project Structure

This is a two-module Maven/Gradle project with a Java bytecode manipulation agent:

```
Bytecode-Manipulation/
├── src/                  # Main application (uses the agent)
│   └── main/java/net/luis/
│       ├── Main.java (entry point with various @annotations)
│       └── Testing.java (test harness)
├── agent/                # Bytecode manipulation agent module
│   ├── build.gradle
│   └── src/main/java/net/luis/agent/
│       ├── Main.java (agent premain entry point)
│       ├── Agent.java (runtime class cache)
│       ├── annotation/      # Annotation definitions
│       ├── asm/             # ASM-related code (core refactoring area)
│       │   ├── data/        # CUSTOM DATA SYSTEM (to be replaced)
│       │   ├── base/        # Base classes for transformers
│       │   ├── scanner/     # Bytecode scanning/reading
│       │   ├── transformer/ # Bytecode transformation
│       │   ├── generation/  # Runtime class generation
│       │   ├── report/      # Error reporting
│       │   ├── type/        # Type system enums
│       │   ├── signature/   # Signature handling
│       │   ├── Instrumentations.java (utility methods)
│       │   ├── Types.java (type helpers)
│       │   └── ASMUtils.java
│       └── util/
└── gradle.properties     # ASM version 9.5
```

## 1. Overall Project Purpose

This is a **bytecode manipulation framework** that:
- Acts as a Java agent (loaded via `-javaagent:agent.jar`)
- Scans and modifies bytecode at class load time
- Implements custom annotations through bytecode transformation
- Uses ASM library (version 9.5) for bytecode manipulation

## 2. The "Data System" - Current Implementation

**Location:** `/agent/src/main/java/net/luis/agent/asm/data/`

This is a **custom in-memory representation of Java bytecode** that mirrors the structure of a compiled class file. It's an abstraction layer between the ASM Classic API and the transformation logic.

### Data Classes (9 classes):

1. **ASMData** (interface) - Base interface with common methods
   - `getName()`, `getType()`, `getSignature(SignatureType)`, `getAccess()`, `getModifiers()`, `getAnnotations()`
   - Functional methods for checking annotations, access, modifiers

2. **Class** - Represents a Java class
   - Fields: name, type, genericSignature, access, classType, modifiers, superType, permittedSubclasses, interfaces
   - Collections: annotations, recordComponents, fields, methods, innerClasses
   - Builder pattern for construction

3. **Method** - Represents a method within a class
   - Fields: owner (Class type), name, type, genericSignature, access, methodType, modifiers
   - Collections: annotations, parameters (by index), exceptions, localVariables
   - Special: annotationDefault (for annotation default values)
   - Mutable methodType that can change (e.g., CONSTRUCTOR -> PRIMARY_CONSTRUCTOR)

4. **Field** - Represents a field within a class
   - Fields: owner, name, type, genericSignature, access, modifiers
   - Collections: annotations
   - Special: initialValue (nullable)

5. **Parameter** - Represents a method parameter
   - Fields: owner (Method), name, type, index, modifiers
   - Collections: annotations
   - Methods to determine if named vs auto-generated (arg0, arg1, etc.)

6. **LocalVariable** - Represents a local variable in a method
   - Fields: owner (Method), index, name, type, genericSignature, scope
   - Collections: annotations
   - Inner class: Scope (start/end indices)

7. **Annotation** - Represents a Java annotation
   - Fields: type, visible (runtime vs compile-time)
   - Collections: values (Map<String, Object> for annotation properties)
   - Methods to get values with defaults from annotation class definition

8. **RecordComponent** - Represents a record component
   - Fields: owner, name, type, genericSignature
   - Collections: annotations

9. **InnerClass** - Represents an inner/nested class
   - Fields: owner, name, type, access, classType, modifiers
   - Inner class type determines if it's a true inner class or anonymous/lambda

### Type System (Supporting Enums):

- **TypeAccess** (enum): PUBLIC, PROTECTED, PACKAGE, PRIVATE → maps to ASM Opcodes
- **TypeModifier** (enum): STATIC, FINAL, SYNCHRONIZED, VOLATILE, TRANSIENT, ABSTRACT, NATIVE, etc.
- **ClassType** (enum): CLASS, INTERFACE, ENUM, RECORD, ANNOTATION, MODULE
- **MethodType** (enum): CONSTRUCTOR, PRIMARY_CONSTRUCTOR, STATIC_INITIALIZER, METHOD
- **InnerClassType** (enum): INNER, ANONYMOUS, LOCAL
- **SignatureType** (enum): GENERIC, FULL, DEBUG, SOURCE (different string representations)

## 3. Current ASM Usage Pattern

### 3A. ASM Classic API Usage (Visitor Pattern)

The project **exclusively uses ASM Classic API** via visitors:

**Reading/Scanning (ClassVisitor-based):**
- `ClassScanner` extends `ClassVisitor` - visits class bytecode and populates the data system
- `MethodScanner` extends `MethodVisitor` - visits method bytecode
- `AnnotationScanner` extends `AnnotationVisitor` - visits annotations
- `TargetClassScanner` - specialized scanner for finding transformation targets
- `ClassFileScanner` - utility to read class files and apply visitors

**Writing/Transforming (ClassVisitor chain):**
- `BaseClassTransformer` implements `ClassFileTransformer`
  - Uses `ClassReader` to read bytes
  - Creates `ClassWriter` to write bytes
  - Chains `ClassVisitor` implementations
  - Pattern: `reader.accept(visitor, flags)` → `writer.toByteArray()`
- `ContextBasedClassVisitor` extends `ClassVisitor` - base for custom visitors
- `MethodOnlyClassVisitor` extends `ContextBasedClassVisitor` - handles only method transformation
- Specific transformers (ModifyTransformer, InterfaceTransformer, etc.) extend these

**ASM Commons Usage:**
- `LocalVariablesSorter` - reorders local variables (used in method transformers)

### 3B. No ASM Tree API Usage

**Zero usage of tree API classes:**
- No `ClassNode`, `MethodNode`, `FieldNode` imports found
- No `tree.*` imports at all
- All bytecode reading/writing done through visitors (streaming)

### 3C. Manual Bytecode Writing

- `Generator` abstract class and subclasses manually write bytecode
- Uses `ClassVisitor` and `MethodVisitor` directly with opcode calls
- Example: `MemorizedSupplierGenerator`, `RuntimeUtilsGenerator`

## 4. Key Components That Would Need Refactoring

### Scanner Components (Reading):
1. **ClassScanner** - converts ClassVisitor callbacks to data classes
2. **MethodScanner** - converts MethodVisitor callbacks to data classes
3. **AnnotationScanner** - converts AnnotationVisitor callbacks to annotation data
4. **TargetClassScanner** - variant that finds transformation targets
5. **ClassFileScanner** - reads .class files and applies visitors

### Transformer Components (Writing):
1. **BaseClassTransformer** - abstract base for all transformers
2. **ContextBasedClassVisitor** - wraps ClassVisitor for context
3. **MethodOnlyClassVisitor** - specialized for method-only transforms
4. All specific transformers:
   - `ModifyTransformer`
   - `InterfaceTransformer`
   - `InterfaceInjectionTransformer`
   - `ImplementedTransformer`
   - `InvokerTransformer`
   - `AssignorTransformer`
   - `AccessorTransformer`
   - `RedirectTransformer`
   - Method-specific: `StringTransformer`, `PatternTransformer`, `RangeTransformer`, etc.

### Generation Components:
1. **Generator** - abstract base
2. Generator implementations - all use manual MethodVisitor API
3. **GenerationLoader** - loads generated classes

### Data System Components:
- All 9 data classes in `/asm/data/` package
- Would need equivalent or modified versions with tree API

## 5. Data Flow

```
Bytecode (byte[])
    ↓
[ClassReader] (ClassVisitor consumer)
    ↓
[ClassScanner] (extends ClassVisitor)
    ├→ [MethodScanner] (extends MethodVisitor)
    ├→ [AnnotationScanner] (extends AnnotationVisitor)
    ↓
[Data System Objects] (Class, Method, Field, Parameter, LocalVariable, etc.)
    ↓
[Transformers] (extend ClassVisitor/MethodVisitor)
    ├→ Read from data system
    ├→ Emit bytecode instructions via MethodVisitor
    ↓
[ClassWriter] (collects visited bytecode)
    ↓
Bytecode (byte[])
```

## 6. Dependencies

- **ASM 9.5** (Classic & Commons APIs)
  - `org.ow2.asm:asm:9.5`
  - `org.ow2.asm:asm-commons:9.5`
- **LUtils 5.13.0** - custom utility library
- **Guava 33.0.0** - collections utilities
- **Log4j 2.22.1** - logging
- **JetBrains Annotations 24.1.0** - @NotNull, @Nullable

## 7. Current State (from git log)

Recent commits show:
- `7f08041`: Updated ModifyTransformer
- `b7fc284`: Cached changes
- `99dff2c`: Added support for list, set, map to string factory
- `2ca77e0`: Updated LUtils to version 5.13.0
- `3aae648`: Removed null caught action

Project is actively maintained and being enhanced.

## 8. Key Characteristics of Data System

1. **Hierarchical Structure:**
   - Class contains Methods, Fields, InnerClasses, RecordComponents
   - Method contains Parameters and LocalVariables
   - All can contain Annotations

2. **Immutable Design:**
   - All data classes use final fields
   - Builder pattern for construction
   - No setters - modifications create new instances via builders

3. **Signature Support:**
   - Multiple signature types: GENERIC, FULL, DEBUG, SOURCE
   - Useful for error messages and debugging

4. **Type System Integration:**
   - TypeAccess (PUBLIC/PROTECTED/PACKAGE/PRIVATE)
   - TypeModifier set (STATIC, FINAL, ABSTRACT, etc.)
   - Opcodes are derived from access/modifiers

5. **ASMData Interface:**
   - Common interface for all major data types
   - Provides utility methods for common operations
   - Good abstraction point

## 9. Refactoring Opportunities with Tree API

### Why Tree API Would Help:
1. **Single representation:** ClassNode encapsulates entire class bytecode
2. **Simpler modifications:** Change tree, then write back
3. **Random access:** Visit methods in any order (vs visitor streaming)
4. **Multiple passes:** Could process tree multiple times
5. **Better debugging:** Can inspect entire tree structure before writing

### Migration Strategy Would Need:
1. Replace ClassScanner with ClassNode usage
2. Update transformers to work with ClassNode/MethodNode
3. Adapt data system or bridge to tree API
4. Update generators to use tree API

## Summary

The project has a well-architected custom data system that abstracts Java bytecode structure. It's entirely based on ASM Classic API with no tree API usage. The data system is immutable, hierarchical, and well-designed for the transformation logic. Refactoring to tree API would require updating the scanning/reading logic, transformer logic, and generation components while potentially maintaining or redesigning the data system abstraction.
