# Access
 - `ACC_PUBLIC` (0x0001):\
 The class, field, or method is declared public and can be accessed from other classes.
 - `ACC_PRIVATE` (0x0002):\
 The class, field, or method is declared private, and can only be accessed within its own class.
 - `ACC_PROTECTED` (0x0004):\
 The class, field, or method is declared protected, and can be accessed within its own package, subclasses, and classes in the same package.
 - `ACC_STATIC` (0x0008):\
 The field or method is declared static, meaning it belongs to the class rather than an instance of the class.
 - `ACC_FINAL` (0x0010):\
 The class, field, method, or parameter is declared final, meaning it cannot be subclassed, overridden, or reassigned, respectively.
 - `ACC_SUPER` (0x0020):\
 For classes, this flag is used for invoking superclass methods in the Java programming language.
 - `ACC_SYNCHRONIZED` (0x0020):\
 For methods, this flag indicates the method is declared synchronized, meaning only one thread can execute it at a time.
 - `ACC_OPEN` (0x0020):\
 This flag is used to indicate that a module is open. An open module allows all types in its module declaration to be accessed by reflection, regardless of the exports declarations.
 - `ACC_TRANSITIVE` (0x0020):\
 This flag is used to indicate that a module is transitive. A transitive module is one that is required by another module, and its requires directive is automatically read by any module that requires the module that declares the transitive directive.
 - `ACC_VOLATILE` (0x0040):\
 For fields, this flag indicates the field is declared volatile, meaning it can be accessed and modified by multiple threads.
 - `ACC_BRIDGE` (0x0040):\
 This flag is used to indicate a bridge method, which is a synthetic method created by the Java compiler for various reasons, such as to support covariant return types.
 - `ACC_STATIC_PHASE` (0x0040):\
 This flag is used to indicate that a module requires another module during the static phase. This is a specific feature of the Java Platform Module System (JPMS).
 - `ACC_VARARGS` (0x0080):\
 This flag is used to indicate a method that accepts a variable number of arguments.
 - `ACC_TRANSIENT` (0x0080):\
 For fields, this flag indicates the field is declared transient, meaning it is not serialized when the class is serialized.
 - `ACC_NATIVE` (0x0100):\
 For methods, this flag indicates the method is declared native, meaning it is implemented in a language other than Java.
 - `ACC_INTERFACE` (0x0200):\
 For classes, this flag indicates the class is an interface, not a regular class or an enum.
 - `ACC_ABSTRACT` (0x0400):\
 For classes or methods, this flag indicates the class or method is declared abstract, meaning it cannot be instantiated or it is a method without an implementation.
 - `ACC_STRICT` (0x0800):\
 For methods, this flag indicates the method is declared with strict floating point precision.
 - `ACC_SYNTHETIC` (0x1000):\
 For classes, fields, methods, parameters, or modules, this flag indicates the element is synthetic, meaning it was not present in the source code but was added by the compiler.
 - `ACC_ANNOTATION` (0x2000):\
 For classes, this flag indicates the class is an annotation.
 - `ACC_ENUM` (0x4000):\
 For classes or fields, this flag indicates the class is an enum or the field is an enum constant.
 - `ACC_MANDATED` (0x8000):\
 This flag is used to indicate a mandated element, such as a parameter implicitly declared in a lambda expression, or a module or module member that is mandated by the Java Language Specification. For example, the java.base module is mandated, as are the requires java.base directives of all modules.
 - `ACC_MODULE` (0x8000):\
 For classes, this flag indicates the class is a module declaration.
 - `ACC_RECORD` (0x10000):\
 This flag is used to indicate that the class is a record class. Record classes are a special kind of class introduced in Java 14 as a preview feature, which are used to model plain data aggregates with less ceremony than regular classes.
 - `ACC_DEPRECATED` (0x20000):\
 This flag is used to indicate that the class, field, or method is marked as deprecated. Deprecated classes, fields, or methods are not recommended for use, typically because they have been superseded by newer forms or they are dangerous.

# Load value
 - `ILOAD`:\
 Loads an int value from a local variable onto the operand stack.
 - `LLOAD`:\
 Loads a long value from a local variable onto the operand stack.
 - `FLOAD`:\
 Loads a float value from a local variable onto the operand stack.
 - `DLOAD`:\
 Loads a double value from a local variable onto the operand stack.
 - `ALOAD`:\
 Loads a reference (object) from a local variable onto the operand stack.

# Load array element
 - `IALOAD`:\
 Loads an int value from an array. The array reference and the index are popped from the operand stack.
 - `LALOAD`:\
 Loads a long value from an array. The array reference and the index are popped from the operand stack.
 - `FALOAD`:\
 Loads a float value from an array. The array reference and the index are popped from the operand stack.
 - `DALOAD`:\
 Loads a double value from an array. The array reference and the index are popped from the operand stack.
 - `AALOAD`:\
 Loads a reference (object) from an array. The array reference and the index are popped from the operand stack.
 - `BALOAD`:\
 Loads a byte or boolean value from an array. The array reference and the index are popped from the operand stack.
 - `CALOAD`:\
 Loads a char value from an array. The array reference and the index are popped from the operand stack.
 - `SALOAD`:\
 Loads a short value from an array. The array reference and the index are popped from the operand stack.