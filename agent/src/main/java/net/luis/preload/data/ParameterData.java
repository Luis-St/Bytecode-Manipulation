package net.luis.preload.data;

import net.luis.preload.type.TypeModifier;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public record ParameterData(String name, int index, List<TypeModifier> modifiers, List<AnnotationData> annotations) {}
