package net.luis.preload.scanner;

import net.luis.preload.data.AnnotationScanData;
import net.luis.preload.data.type.TypeModifier;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public record ParameterScanData(String name, int index, List<TypeModifier> modifiers, List<AnnotationScanData> annotations) {}
