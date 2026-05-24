package com.arcadesmasher.guiinspector;

import java.util.List;
import java.util.Map;

public class Descriptors {

	private static final Map<String, String> PRIMITIVES = Map.ofEntries(
			Map.entry("void", "V"),
			Map.entry("boolean", "Z"),
			Map.entry("byte", "B"),
			Map.entry("char", "C"),
			Map.entry("short", "S"),
			Map.entry("int", "I"),
			Map.entry("long", "J"),
			Map.entry("float", "F"),
			Map.entry("double", "D")
	);

	public static String typeDescriptor(String type) {
		int dims = 0;

		while (type.endsWith("[]")) {
			dims++;
			type = type.substring(0, type.length() - 2);
		}

		String base = PRIMITIVES.get(type);

		if (base == null) {
			base = "L" + type.replace('.', '/') + ";";
		}

		return "[".repeat(dims) + base;
	}

	public static String methodDescriptor(String returnType, List<String> parameterTypes) {
		StringBuilder sb = new StringBuilder();

		sb.append('(');

		for (String param : parameterTypes) {
			sb.append(typeDescriptor(param));
		}

		sb.append(')');
		sb.append(typeDescriptor(returnType));

		return sb.toString();
	}

	public static String fullMethodDescriptor(String ownerClass, String methodName, String returnType, List<String> parameterTypes) {
		return "L" + ownerClass.replace('.', '/') + ";" + methodName + methodDescriptor(returnType, parameterTypes);
	}

	public static String getDescriptorFromFrame(StackWalker.StackFrame frame) {
		return Descriptors.fullMethodDescriptor(
				GUIInspector.getMappedClassName(frame.getDeclaringClass()),
				GUIInspector.getMappedMethodName(frame.getDeclaringClass(), frame.getMethodName()),
				GUIInspector.getMappedClassName(frame.getMethodType().returnType()),
				frame.getMethodType().parameterList().stream().map(GUIInspector::getMappedClassName).toList()
		);
	}
}