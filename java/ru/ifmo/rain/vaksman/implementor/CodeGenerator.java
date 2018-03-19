package ru.ifmo.rain.vaksman.implementor;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Collectors;

public class CodeGenerator {
    private String newLine;
    private String tab;
    private String className;

    public CodeGenerator(String newLine, String tab) {
        this.newLine = newLine;
        this.tab = tab;
    }

    public String generateClassDeclaration(Class<?> clazz) {
        className = clazz.getSimpleName() + "Impl";
        return "package " + clazz.getPackage().getName() + ";" + newLine + newLine
                + "public class " + className + " " + (clazz.isInterface() ? "implements" : "extends") + " " + clazz.getCanonicalName() + " {" + newLine;
    }

    public String generateEndOfClassDeclaration() {
        return "}" + newLine + newLine;
    }

    public String generateMethod(Executable m, boolean isConstructor) {
        return  modifiers(m) + " "
                + (!isConstructor ? retType((Method) m) + " " : "")
                + name(m, isConstructor)
                + "(" + parameters(m) + ") "
                + exceptions(m)
                + " {" + newLine
                + body(m, isConstructor)
                + "}"
                + newLine + newLine;
    }

    private String modifiers(Executable m) {
        return Modifier.toString(m.getModifiers()).replaceAll("abstract|transient|volatile|native", "");
    }

    private String retType(Method m) {
        return m.getReturnType().getCanonicalName();
    }

    private String name(Executable m, boolean isConstructor) {
        if (isConstructor)
                return className;
        return m.getName();
    }

    private String parameters(Executable m) {
        return String.join(", ",
                                    Arrays.stream(m.getParameters())
                                        .map(p -> p.getType().getCanonicalName() + " " + p.getName())
                                        .collect(Collectors.toList()));
    }

    private String exceptions(Executable m) {
        Class<?>[] e = m.getExceptionTypes();
        if (e.length == 0)
                return "";
        return "throws "
                + String.join(", ", Arrays.stream(e)
                                            .map(Class::getCanonicalName)
                                            .collect(Collectors.toList()));
    }

    private String body(Executable m, boolean isConstructor) {
        if (isConstructor) {
            return tab + "super("
                    + String.join(", ",
                                        Arrays.stream(m.getParameters())
                                            .map(Parameter::getName)
                                            .collect(Collectors.toList()))
                    + ");";
        }
        return tab + "return "
                + getDefaultValue(((Method) m).getReturnType()) + ";"
                + newLine;
    }

    private String getDefaultValue(Class<?> clazz) {
        String typeName = clazz.getCanonicalName();
        if (typeName.equals("byte"))
            return "0";
        if (typeName.equals("short"))
            return "0";
        if (typeName.equals("int"))
            return "0";
        if (typeName.equals("long"))
            return "0L";
        if (typeName.equals("char"))
            return "'\u0000'";
        if (typeName.equals("float"))
            return "0.0F";
        if (typeName.equals("double"))
            return "0.0";
        if (typeName.equals("boolean"))
            return "false";
        if (typeName.equals("void"))
            return "";
        return "null";
    }
}
