package net.luis.preload.scanner;

import net.luis.preload.data.AnnotationData;
import net.luis.preload.data.type.TypeModifier;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public record ParameterScanData(String name, int index, List<TypeModifier> modifiers, List<AnnotationData> annotations) {}
