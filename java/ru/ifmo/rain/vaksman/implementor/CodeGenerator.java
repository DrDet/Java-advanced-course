package ru.ifmo.rain.vaksman.implementor;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * This class provides methods for code generation.
 *
 * @author Denis Vaksman
 */
public class CodeGenerator {

    /**
     * New line character for generated code.
     */
    private String newLine;

    /**
     * Tab character for generated code.
     */
    private String tab;

    /**
     * Name of generated class.
     */
    private String className;

    /**
     * Constructs CodeGenerator instance with given characters.
     *
     * @param className name of implemented class
     * @param newLine new line character
     * @param tab tab character
     */
    public CodeGenerator(String className, String newLine, String tab) {
        this.className = className;
        this.newLine = newLine;
        this.tab = tab;
    }

    /**
     * Generates declaration of given class, e.g. package description
     * and first line of defining java class, which contains modifiers, name, implemented interfaces and superclasses.
     *
     * @param clazz class to generate declaration for
     * @return {@link String} representation of declaration
     */
    public String generateClassDeclaration(Class<?> clazz) {
        return (clazz.getPackage() != null ? "package " + clazz.getPackage().getName() + ";" + newLine + newLine : "")
                + "public class " + className + " " + (clazz.isInterface() ? "implements" : "extends") + " " + clazz.getCanonicalName() + " {" + newLine;
    }

    /**
     * Generates last line of class declaration, e.g. close bracket and {@link #newLine} twice.
     * @return {@link String} representation of end of declaration
     */
    public String generateEndOfClassDeclaration() {
        return "}" + newLine + newLine;
    }

    /**
     * Generates implementation of given method or constructor.
     *
     * @param m given method or constructor to implement
     * @param name name of given method or constructor
     * @param retType return type of given method or empty string for constructor
     * @param body body to generate
     * @return string representation of method or constructor code
     */
    private String generate(Executable m, String name, String retType, String body) {
        return  tab + modifiers(m) + " "
                + retType + " "
                + name
                + "(" + parameters(m) + ") "
                + exceptions(m)
                + " {" + newLine
                + tab + tab + body + newLine
                + tab + "}"
                + newLine + newLine;
    }

    /**
     * Generates body of given method.
     *
     * @param m given method to implement
     * @return string representation of body
     */
    private String body(Method m) {
        return tab + "return "
                + getDefaultValue(m.getReturnType()) + ";";
    }

    /**
     * Generates body of given constructor.
     *
     * @param c given constructor to implement
     * @return string representation of body
     */
    private String body(Constructor<?> c) {
        return tab + "super("
                + String.join(", ",
                Arrays.stream(c.getParameters())
                        .map(Parameter::getName)
                        .collect(Collectors.toList()))
                + ");";
    }

    /**
     * Generates implementation of given method.
     *
     * @param m given method or constructor to implement
     * @return string representation of method code
     */
    public String generateMethod(Method m) {
        return generate(m, m.getName(), retType(m), body(m));
    }

    /**
     * Generates implementation of given constructor.
     *
     * @param c given constructor to implement
     * @return string representation of constructor code
     */
    public String generateConstructor(Constructor<?> c) {
        return generate(c, className,"", body(c));
    }

    /**
     * Generate modifiers for given method.
     *
     * @param m method to generate modifiers for
     * @return string representation of modifiers
     */
    private String modifiers(Executable m) {
        return Modifier.toString(m.getModifiers()).replaceAll("abstract|transient|volatile|native", "");
    }

    /**
     * Generate return type for given method.
     *
     * @param m method to generate return type for
     * @return string representation of return type
     */
    private String retType(Method m) {
        return m.getReturnType().getCanonicalName();
    }

    /**
     * Generate parameters for given method.
     *
     * @param m method to generate parameters for
     * @return string representation of parameters
     */
    private String parameters(Executable m) {
        return String.join(", ",
                                    Arrays.stream(m.getParameters())
                                        .map(p -> p.getType().getCanonicalName() + " " + p.getName())
                                        .collect(Collectors.toList()));
    }

    /**
     * Generate throwing exceptions for given method.
     *
     * @param m method to generate throwing exceptions for
     * @return string representation of throwing exceptions
     */
    private String exceptions(Executable m) {
        Class<?>[] e = m.getExceptionTypes();
        if (e.length == 0)
                return "";
        return "throws "
                + String.join(", ", Arrays.stream(e)
                                            .map(Class::getCanonicalName)
                                            .collect(Collectors.toList()));
    }

    /**
     * Generates string representation of default value of given class.
     *
     * @param clazz class to generate default value string representation for
     * @return string representation of default value
     */
    private String getDefaultValue(Class<?> clazz) {
        String typeName = clazz.getCanonicalName();
        if (typeName.equals("byte") || typeName.equals("short") || typeName.equals("int"))
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
