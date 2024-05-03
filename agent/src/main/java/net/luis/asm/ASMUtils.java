package net.luis.asm;

import net.luis.preload.ClassDataPredicate;
import net.luis.preload.PreloadContext;
import net.luis.preload.data.MethodData;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static net.luis.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class ASMUtils {
	
	public static @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> createTargetsLookup(@NotNull PreloadContext context, @NotNull Type annotationType) {
		Map<String, List<String>> lookup = new HashMap<>();
		context.stream().filter(ClassDataPredicate.annotatedWith(annotationType)).forEach((info, content) -> {
			List<Type> types = info.getAnnotation(annotationType).get("targets");
			for (Type target : types) {
				lookup.computeIfAbsent(target.getInternalName(), k -> new ArrayList<>()).add(info.type().getInternalName());
			}
		});
		return lookup;
	}
	
	public static void saveClass(@NotNull File file, byte @NotNull [] data) {
		try {
			Files.deleteIfExists(file.toPath());
			Files.createDirectories(file.getParentFile().toPath());
			Files.write(file.toPath(), data);
		} catch (Exception e) {
			System.err.println("Failed to save class file: " + file.getName());
		}
	}
	
	public static  @NotNull String getReturnTypeSignature(@NotNull MethodData method) {
		String signature = method.signature();
		if (signature == null || signature.isEmpty()) {
			return "";
		}
		int index = signature.indexOf(')');
		return signature.substring(index + 1);
	}
	
	public static @NotNull String getParameterTypesSignature(@NotNull MethodData method) {
		String signature = method.signature();
		if (signature == null || signature.isEmpty()) {
			return "";
		}
		int start = signature.indexOf('(');
		int end = signature.indexOf(')');
		return signature.substring(start + 1, end);
	}
	
	public static void addMethodAnnotations(@NotNull MethodVisitor methodVisitor, @NotNull MethodData method) {
		addMethodAnnotations(methodVisitor, method, true);
	}
	
	public static void addMethodAnnotations(@NotNull MethodVisitor methodVisitor, @NotNull MethodData method, boolean generated) {
		if (generated) {
			AnnotationVisitor annotationVisitor = methodVisitor.visitAnnotation(GENERATED.getDescriptor(), true);
			if (annotationVisitor != null) {
				annotationVisitor.visitEnd();
			}
		}
		method.getAnnotations().forEach(annotation -> {
			if (annotation.type().equals(GENERATED) || IMPLEMENTATION_ANNOTATIONS.contains(annotation.type())) {
				return;
			}
			AnnotationVisitor annotationVisitor = methodVisitor.visitAnnotation(annotation.type().getDescriptor(), true);
			if (annotationVisitor != null) {
				annotationVisitor.visitEnd();
			}
		});
	}
	
	public static void addParameterAnnotations(@NotNull MethodVisitor methodVisitor, @NotNull MethodData method) {
		method.parameters().forEach(parameter -> {
			parameter.getAnnotations().forEach(annotation -> {
				AnnotationVisitor annotationVisitor = methodVisitor.visitParameterAnnotation(parameter.index(), annotation.type().getDescriptor(), true);
				if (annotationVisitor != null) {
					annotationVisitor.visitEnd();
				}
			});
		});
	}
}
