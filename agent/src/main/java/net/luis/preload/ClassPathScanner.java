package net.luis.preload;

import org.objectweb.asm.Type;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 *
 * @author Luis-St
 *
 */

public class ClassPathScanner {
	
	public static List<Type> getClasses() {
		List<Type> classes = new ArrayList<>();
		for (File file : getClassPathFiles()) {
			if (file.isDirectory()) {
				classes.addAll(getClassesFromDirectory(file));
			} else {
				classes.addAll(getClassesFromJar(file));
			}
		}
		return classes;
	}
	
	private static List<Type> getClassesFromJar(File file) {
		List<Type> classes = new ArrayList<>();
		if (file.exists() && file.canRead()) {
			try (JarFile jar = new JarFile(file)) {
				Enumeration<JarEntry> enumeration = jar.entries();
				while (enumeration.hasMoreElements()) {
					JarEntry entry = enumeration.nextElement();
					if (entry.getName().endsWith(".class")) {
						classes.add(Type.getObjectType(convertToClass(entry.getName())));
					}
				}
			} catch (Exception ignored) {}
		}
		return classes;
	}
	
	private static List<Type> getClassesFromDirectory(File directory) {
		List<Type> classes = new ArrayList<>();
		for (File file : listFiles(directory, (dir, name) -> name.endsWith(".jar"))) {
			classes.addAll(getClassesFromJar(file));
		}
		for (File classfile : listFiles(directory, (dir, name) -> name.endsWith(".class"))) {
			String className = convertToClass(classfile.getAbsolutePath().substring(directory.getAbsolutePath().length() + 1));
			classes.add(Type.getObjectType(className));
		}
		return classes;
	}
	
	private static List<File> listFiles(File directory, FilenameFilter filter) {
		List<File> files = new ArrayList<>();
		for (File entry : Objects.requireNonNull(directory.listFiles())) {
			if (filter == null || filter.accept(directory, entry.getName())) {
				files.add(entry);
			}
			if (entry.isDirectory()) {
				files.addAll(listFiles(entry, filter));
			}
		}
		return files;
	}
	
	private static List<File> getClassPathFiles() {
		List<File> files = new ArrayList<>();
		String classPath = System.getProperty("java.class.path");
		if (classPath != null) {
			for (String path : classPath.split(File.pathSeparator)) {
				files.add(new File(path));
			}
		}
		return files;
	}
	
	private static String convertToClass(String fileName) {
		return fileName.substring(0, fileName.length() - 6).replace("\\", "/");
	}
}
