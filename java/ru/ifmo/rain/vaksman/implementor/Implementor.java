
package ru.ifmo.rain.vaksman.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class Implementor implements Impler {
    private Path srcFile;

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token.isPrimitive() || token.isArray() || Modifier.isFinal(token.getModifiers()) || token.equals(java.lang.Enum.class)) {
            throw new ImplerException("The given token is incorrect.");
        }
        try {
            createFile(token, root);
        } catch (IOException e) {
            throw new ImplerException("Couldn't create a source file", e);
        }
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(srcFile), "UTF-8"))
        {
            Set<Integer> methods = new TreeSet<>();
            CodeGenerator generator = new CodeGenerator(System.lineSeparator(), "    ");
            writer.write(generator.generateClassDeclaration(token));
            for (Method m : token.getMethods()) {
                if (Modifier.isAbstract(m.getModifiers()) && !methods.contains(getHash(m))) {
                    writer.write(generator.generateMethod(m, false));
                    methods.add(getHash(m));
                }
            }
            if (!token.isInterface()) {
                boolean canExtend = false;
                for (Constructor<?> c : token.getDeclaredConstructors()) {
                    if (!Modifier.isPrivate(c.getModifiers())) {
                        writer.write(generator.generateMethod(c, true));
                        canExtend = true;
                    }
                }
                if (!canExtend) throw new ImplerException("Couldn't access constructors of super class");
                Class<?> cur = token;
                while (cur != null && Modifier.isAbstract(cur.getModifiers())) {
                    for (Method m : cur.getDeclaredMethods()) {
                        int mod = m.getModifiers();
                        if (Modifier.isAbstract(mod) && !(Modifier.isPublic(mod) || Modifier.isPrivate(mod)) && !methods.contains(getHash(m))) {
                            writer.write(generator.generateMethod(m, false));
                            methods.add(getHash(m));
                        }
                    }
                    cur = cur.getSuperclass();
                }
            }
            writer.write(generator.generateEndOfClassDeclaration());
        } catch (IOException e) {
            throw new ImplerException("Couldn't write to the source file", e);
        }
    }

    private void createFile(Class<?> token, Path root) throws IOException {
        srcFile = Paths.get(
                root
                        + File.separator
                        + token.getCanonicalName()
                        .replace(".", File.separator)
                        + "Impl.java");
        Files.createDirectories(srcFile.getParent());
        Files.createFile(srcFile);
    }

    private int getHash(Method m) {
        return (m.getName() + Arrays.toString(m.getParameterTypes())).hashCode();
    }
}
