# Key Files Reference Guide

## Data System Components
```
/agent/src/main/java/net/luis/agent/asm/data/
├── ASMData.java                 Interface; 69 lines; all common methods
├── Class.java                   13,858 bytes; represents java class; complex builder
├── Method.java                  13,158 bytes; represents method; has mutable methodType
├── Field.java                   7,715 bytes; represents field; simple
├── Parameter.java               6,723 bytes; method parameter; knows if named/auto
├── LocalVariable.java           7,507 bytes; local variable in method; has scope
├── Annotation.java              4,354 bytes; annotation instance; holds values
├── RecordComponent.java         4,858 bytes; record field; simple
└── InnerClass.java              5,504 bytes; nested class; inner/anon/local types
```

## Type System Components
```
/agent/src/main/java/net/luis/agent/asm/type/
├── TypeAccess.java              Enum: PUBLIC, PROTECTED, PACKAGE, PRIVATE
├── TypeModifier.java            Enum: STATIC, FINAL, ABSTRACT, etc. (15 modifiers)
├── ClassType.java               Enum: CLASS, INTERFACE, ENUM, RECORD, ANNOTATION, MODULE
├── MethodType.java              Enum: CONSTRUCTOR, PRIMARY_CONSTRUCTOR, STATIC_INITIALIZER, METHOD
├── InnerClassType.java          Enum: INNER, ANONYMOUS, LOCAL
└── SignatureType.java           Enum: GENERIC, FULL, DEBUG, SOURCE
```

## Scanner Components (Reading/Parsing)
```
/agent/src/main/java/net/luis/agent/asm/scanner/
├── ClassScanner.java            Extends ClassVisitor; populates Class data
├── MethodScanner.java           Extends MethodVisitor; populates Method data
├── AnnotationScanner.java       Extends AnnotationVisitor; populates Annotation data
├── TargetClassScanner.java      Specialized scanner for finding transform targets
├── ClassFileScanner.java        Utility; loads .class files and applies visitors
├── ClassPathScanner.java        Class path scanning utilities
└── (supporting files)
```

## Transformer Components (Writing/Modifying)
```
/agent/src/main/java/net/luis/agent/asm/transformer/
├── implementation/
│   ├── ModifyTransformer.java           Injects modify interface implementations
│   ├── InterfaceTransformer.java        Base interface handling
│   ├── InterfaceInjectionTransformer.java Injects interfaces into classes
│   ├── ImplementedTransformer.java      Implements marked interfaces
│   ├── InvokerTransformer.java          Invokes redirected methods
│   ├── AssignorTransformer.java         Auto-assigns values
│   ├── AccessorTransformer.java         Creates accessors/getters
│   └── RedirectTransformer.java         Redirects method calls
├── method/
│   ├── StringTransformer.java           String transformation
│   ├── PatternTransformer.java          Pattern validation
│   ├── RangeTransformer.java            Range validation
│   ├── NotNullTransformer.java          Null checks
│   ├── RestrictedAccessTransformer.java Access control
│   ├── AsyncTransformer.java            Async/threading
│   ├── ScheduledTransformer.java        Scheduled execution
│   ├── DefaultTransformer.java          Default values
│   └── CaughtTransformer.java           Exception handling
└── (additional transformers)
```

## Base Classes and Utilities
```
/agent/src/main/java/net/luis/agent/asm/base/
├── BaseClassTransformer.java            Abstract ClassFileTransformer; core pattern
├── ContextBasedClassVisitor.java        Extends ClassVisitor; adds context
├── MethodOnlyClassVisitor.java          Extends ContextBasedClassVisitor
├── LabelTrackingMethodVisitor.java      Tracks labels for scoping
└── (helpers)

/agent/src/main/java/net/luis/agent/asm/
├── Instrumentations.java        Static utility methods; bytecode operations
├── Types.java                   Type manipulation utilities
├── ASMUtils.java                ASM-related utilities
└── (utilities)
```

## Generation Components (Runtime Class Creation)
```
/agent/src/main/java/net/luis/agent/asm/generation/
├── Generator.java               Abstract base; takes ClassVisitor
├── GenerationLoader.java        Loads generated classes
├── Generations.java             Constants for generated classes
└── generators/
    ├── RuntimeUtilsGenerator.java       Generates RuntimeUtils class
    ├── MemorizedSupplierGenerator.java  Generates supplier wrapper
    └── concurrent/
        ├── DaemonThreadFactoryGenerator.java
        ├── ContextRunnableGenerator.java
        ├── CountingRunnableGenerator.java
        └── CancelableRunnableGenerator.java
```

## Entry Points
```
/agent/src/main/java/net/luis/agent/
├── Main.java                    Premain entry; initializes agent
├── Agent.java                   Runtime class cache; Agent.getClass(Type)
└── annotation/                  Annotation definitions used by transformers
```

## Critical Usage Patterns

### Pattern 1: Reading with ClassScanner
```java
ClassScanner scanner = new ClassScanner();
ClassReader reader = new ClassReader(bytecode);
reader.accept(scanner, ClassReader.EXPAND_FRAMES);
// scanner now has populated Class data
Class classData = scanner.getClass();
```

### Pattern 2: Writing with ClassWriter
```java
ClassReader reader = new ClassReader(bytecode);
ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
ClassVisitor visitor = new YourTransformer(writer);
reader.accept(visitor, ClassReader.EXPAND_FRAMES);
byte[] newBytecode = writer.toByteArray();
```

### Pattern 3: Data System Access
```java
// Immutable data access
Class classData = ...;
Method method = classData.getMethod(fullSignature);
for (Parameter param : method.getParameters().values()) {
    Type paramType = param.getType();
}

// Modification via builder
Class modified = Class.builder(classData)
    .addModifier(TypeModifier.FINAL)
    .removeField("fieldName")
    .build();
```

### Pattern 4: Generator
```java
public class MyGenerator extends Generator {
    public MyGenerator() {
        super("my.generated.ClassName");
    }
    
    @Override
    public void generate(ClassVisitor cv) {
        cv.visit(CLASS_VERSION, Opcodes.ACC_PUBLIC, 
                 getInternalName(), null, "java/lang/Object", null);
        // ... visit methods, fields
        cv.visitEnd();
    }
}
```

## File Statistics
```
Total Java files in agent: 108
Data system files:         9
Scanner files:            ~6
Transformer files:        ~20
Type system files:        6
Generation files:         ~8
Base/utility files:       ~10
Other:                    ~49
```

## Important Constants
```
ASM Version: 9.5
Class Version: 65 (Java 21)
ASM Opcode Level: Opcodes.ASM9
```

## Module Dependency
```
Main module (src/) depends on:
- agent module
- ASM 9.5
- LUtils 5.13.0
- Guava 33.0.0-jre
- Log4j 2.22.1
- Apache Commons Lang 3.14.0
```
