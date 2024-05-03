package net.luis.asm;

import net.luis.preload.ClassDataPredicate;
import net.luis.preload.PreloadContext;
import net.luis.preload.data.MethodData;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

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
}
