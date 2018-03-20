package ru.ifmo.rain.vaksman.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class Implementor implements JarImpler {
    private Path srcFile;

    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        try {
            Files.createDirectories(jarFile.getParent());
        } catch (IOException e) {
            throw new ImplerException("Couldn't create jar file", e);
        }
        Path tmpDir;
        try {
            tmpDir = Files.createTempDirectory(jarFile.getParent(), "tmp");
        } catch (IOException e) {
            throw new ImplerException("Couldn't create temp directory", e);
        }
        Implementor implementor = new Implementor();
        implementor.implement(token, tmpDir);
        compile(implementor.srcFile, tmpDir);
        try (JarOutputStream jarOut = new JarOutputStream(Files.newOutputStream(jarFile), createManifest())) {
            try {
                jarOut.putNextEntry(new ZipEntry(token.getName().replace('.', '/') + "Impl.class"));
                Files.copy(tmpDir.resolve(token.getCanonicalName().replace(".", File.separator) + "Impl.class"), jarOut);
            } catch (IOException e) {
                throw new ImplerException("Couldn't write to jar file", e);
            }
        } catch (IOException e) {
            throw new ImplerException("Couldn't create jar file", e);
        } catch (InvalidPathException e) {
            throw new ImplerException("Given jar path is incorrect", e);
        }
    }

    private Manifest createManifest() {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR, "Denis Vaksman");
        return manifest;
    }

    private void compile(Path src, Path root) {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final String[] args = new String[3];
        args[0] = src.toAbsolutePath().toString();
        args[1] = "-cp";
        args[2] = root.toString() + File.pathSeparator + System.getProperty("java.class.path");
        compiler.run(null, null, null, args);
    }

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
            CodeGenerator generator = new CodeGenerator(token.getSimpleName() + "Impl", System.lineSeparator(), "    ");
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
        srcFile = root.resolve(token.getCanonicalName().replace(".", File.separator) + "Impl.java");
        Files.createDirectories(srcFile.getParent());
        Files.createFile(srcFile);
    }

    private int getHash(Method m) {
        return (m.getName() + Arrays.toString(m.getParameterTypes())).hashCode();
    }

    public static void main(String[] args) {
        final String wrongUsageMessage = "Incorrect arguments.\nUsage:\nImplementor <full-class-name> <src-path>\nor\nImplementor -jar <full-class-name> <jar-path>\n";
        if (args == null || args.length != 2 && args.length != 3 || args[0] == null || args[1] == null || args[2] == null) {
            System.out.println(wrongUsageMessage);
            return;
        }
        JarImpler implementor = new Implementor();
        try {
            switch (args.length) {
                case 2:
                    implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
                    break;
                case 3:
                    if (!args[0].equals("-jar")) {
                        System.out.println(wrongUsageMessage);
                        return;
                    }
                    implementor.implement(Class.forName(args[1]), Paths.get(args[2]));
                    break;
            }
        } catch(ImplerException e) {
            System.out.println("Couldn't implement given type:\n" + e.getMessage());
        } catch (InvalidPathException e) {
            System.out.println("Given path is incorrect:\n" + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("Couldn't find given type:\n" + e.getMessage());
        }
    }
}
