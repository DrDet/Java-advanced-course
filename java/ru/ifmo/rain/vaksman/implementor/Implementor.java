
package ru.ifmo.rain.vaksman.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Implementor implements Impler {
    private Path srcFile;
    private String className;
    private static final String newLine = System.lineSeparator();
    private static final String tab = "    ";

    private void createFile(Class<?> token, Path root) throws IOException {
        className = token.getSimpleName() + "Impl";
        srcFile = Paths.get(
                root
                + File.separator
                + token.getCanonicalName()
                        .replace(".", File.separator)
                        + "Impl.java");
        Files.createDirectories(srcFile.getParent());
        Files.createFile(srcFile);
    }

    private void implementInterface(Class<?> clazz, Writer writer) throws IOException {
        for (Method m : clazz.getDeclaredMethods()) {
            if (!m.isDefault())
                writer.write(generateMethod(m));
        }
        for (Class<?> i : clazz.getInterfaces()) {
            implementInterface(i, writer);
        }
    }

    private String generateMethod(Method m) {
        Class<?>[] exceptions = m.getExceptionTypes();
        return  getModifiers(m) + " "
                + m.getReturnType().getCanonicalName() + " "
                + m.getName()
                + "("
                + String.join(", ",
                    Arrays.stream(m.getParameters())
                        .map(p -> p.getType().getCanonicalName() + " " + p.getName())
                        .collect(Collectors.toList()))
                + ") "
                + (exceptions.length != 0 ?
                        "throws "
                        + String.join(", ",
                            Arrays.stream(exceptions)
                                .map(Class::getCanonicalName)
                                .collect(Collectors.toList())) : "")
                + " {" + newLine
                    + tab + "return " + getDefaultValue(m.getReturnType()) + ";" + newLine
                + "}" + newLine + newLine;
    }

    private String getModifiers(Method m) {
        int x = m.getModifiers();
        return (Modifier.isPrivate(x) ? "private " : "") + (Modifier.isProtected(x) ? "protected " : "") + (Modifier.isPublic(x) ? "public " : "")
                + (Modifier.isStatic(x) ? "static " : "") + (Modifier.isFinal(x) ? "final " : "");
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

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token.isArray() || token.isPrimitive()) {
            throw new ImplerException("The given token is incorrect: array or primitive type is provided.");
        }
        try {
            createFile(token, root);
        } catch (IOException e) {
            throw new ImplerException("Couldn't create a source file", e);
        }
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(srcFile), "UTF-8")) {
            writer.write( "package " + token.getPackageName() + ";" + newLine + newLine
                    + "public class " + className + " " + (token.isInterface() ? "implements" : "extends") + " " + token.getCanonicalName() + " {" + newLine);
            implementInterface(token, writer);
            writer.write("}");
        } catch (IOException e) {
            throw new ImplerException("Couldn't write to the source file", e);
        }
    }
}
