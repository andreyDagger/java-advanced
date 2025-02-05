package info.kgeorgiy.ja.matveev.implementor;

import info.kgeorgiy.java.advanced.implementor.BaseImplementorTest;
import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class, that implements interface or class, that is defined by {@code token}
 *
 * @author Andrey Matveev
 * @version 21
 * @see Impler
 * @see JarImpler
 * @since 21
 */
public class Implementor implements Impler, JarImpler {
    /**
     * Number of spaces for one indent in file, generated by {@link Implementor#implement(Class, Path)}.
     */
    private static final int INDENT_SPACES = 4;

    /**
     * Returns some value (in {@link String} format), which can be used for token instantiation.
     *
     * @param token Class token, for which we want to find correct instantiation
     * @return Correct instantiation
     * @since 21
     */
    private String getDefaultValue(Class<?> token) {
        if (!token.isPrimitive()) {
            return null;
        } else if (void.class.equals(token)) {
            return "";
        } else if (boolean.class.equals(token)) {
            return "false";
        } else {
            return "0";
        }
    }

    /**
     * Writes function parameters in format "Type1 name1, Type2 name2, ...".
     *
     * @param writer     Writer that is used to write
     * @param parameters Array of function parameters
     * @throws IOException If writer failed to write
     * @since 21
     */
    private void writeParameters(Writer writer, Parameter[] parameters) throws IOException {
        writer.write("(");
        writer.write(Arrays.stream(parameters)
                .map(p -> String.format("%s %s", p.getType().getCanonicalName(), p.getName()))
                .collect(Collectors.joining(",")));
        writer.write(") ");
    }

    /**
     * Writes function exceptions, that it can throw in format "throws Ex1, Ex2, ...".
     *
     * @param writer     Writer that is used to write
     * @param exceptions Exceptions, that can be thrown by function
     * @throws IOException If writer failed to write
     * @since 21
     */
    private void writeThrowableExceptions(Writer writer, Class<?>[] exceptions) throws IOException {
        if (exceptions.length == 0) {
            return;
        }
        writer.write("throws ");
        writer.write(Arrays.stream(exceptions)
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(",")));
    }

    /**
     * Adds indention in the begining of {@code s}.
     * One indent is {@value INDENT_SPACES} spaces.
     *
     * @param s           String, to which we're adding indention
     * @param indentCount Number of indentions to be added
     * @return Modified string with indentions
     * @since 21
     */
    // :NOTE: может быть, стоило вынести отдельный метод который пишет только отсутп перед строкой
    // :NOTE: потому что иначе получается лишнее пересоздание строки + длинее строки кода, которые пишгут какой-то существенный код
    private String indent(String s, int indentCount) {
        return " ".repeat(indentCount * INDENT_SPACES) + s;
    }

    /**
     * Writes method head in format "public returnType methodName(Type1 name1, ...)".
     *
     * @param writer     Writer that is used to write
     * @param executable Constructor or method for which we're writing head
     * @param returnType Method return Type, for constructors, it must be empty string
     * @param methodName Constructor/Method name
     * @throws IOException If writer fails to write
     * @since 21
     */
    private void writeMethodHead(Writer writer, Executable executable, String returnType, String methodName) throws IOException {
        writer.write(indent(String.format("public %s %s", returnType, methodName), 1));
        Parameter[] parameters = executable.getParameters();
        writeParameters(writer, parameters);
        writeThrowableExceptions(writer, executable.getExceptionTypes());
    }

    /**
     * Writes body of method. The body consists of one return statement, that returns {@link Implementor#getDefaultValue(Class)}
     * Basically, it's just {@code return getDefaultValue(method.getReturnType())}.
     *
     * @param writer Writer that is used to write
     * @param method Method, whose body we're implementing
     * @throws IOException If writer fails to write
     * @since 21
     */
    private void writeReturn(Writer writer, Method method) throws IOException {
        writer.write(
                indent(String.format("return %s;%n", getDefaultValue(method.getReturnType())), 2)
        );
    }

    /**
     * Writes implementation of method.
     *
     * @param writer Writer that is used to write
     * @param method Method, which we're implementing
     * @throws IOException If writer fails to write
     * @since 21
     */
    private void writeMethod(Writer writer, Method method) throws IOException {
        Class<?> returnType = method.getReturnType();

        writeMethodHead(writer, method, returnType.getCanonicalName(), method.getName());
        writer.write(String.format("{%n"));
        writeReturn(writer, method);
        writer.write(indent(String.format("}%n"), 1));
    }

    /**
     * Writes head of class/interface.
     * It will be class or interface depends on your {@code token}.
     * The class name will be equal to {@code String.format("%sImpl", token.getSimpleName())}.
     * The class/interface that our class extends/implements is defined by {@code token}.
     *
     * @param writer Writer that is used to write
     * @param token  Class/interface that our new class extends/interface
     * @throws IOException If writer fails to write
     * @since 21
     */
    private void writeClassHead(Writer writer, Class<?> token) throws IOException {
        String className = String.format("%sImpl", token.getSimpleName());
        writer.write(String.format("%s;%n", token.getPackage()));
        writer.write(String.format("public class %s %s %s {%n",
                className, token.isInterface() ? "implements" : "extends", token.getCanonicalName()));
    }

    /**
     * Returns non-private constructors of given class token.
     * Token must be implementable or extendable, otherwise {@link ImplerException} will be thrown
     *
     * @param token Class, which constructors are returned
     * @return Non-private constructors of {@code token}
     * @throws ImplerException If {@code token} is not interface and has no non-private constructors
     * @since 21
     */
    private List<Constructor<?>> getConstructors(Class<?> token) throws ImplerException {
        List<Constructor<?>> constructors = Arrays.stream(token.getDeclaredConstructors())
                .filter(c -> !Modifier.isPrivate(c.getModifiers()))
                .toList();
        if (!token.isInterface() && constructors.isEmpty()) {
            throw new ImplerException("Can't implement class, that has only private constructors");
        }
        return constructors;
    }

    /**
     * Writes implementation of class {@code token}.
     *
     * @param writer Writer that is used to write
     * @param token  Class that we're extending
     * @throws IOException     If writer fails to write
     * @throws ImplerException If it's impossible to extend {@code token}: some methods have private return type, or private arguments, that we can't access
     * @since 21
     */
    private void implementClass(Writer writer, Class<?> token) throws IOException, ImplerException {
        String className = String.format("%sImpl", token.getSimpleName());
        writeClassHead(writer, token);

        List<Constructor<?>> constructors = getConstructors(token);

        for (Constructor<?> constructor : constructors) {
            writeMethodHead(writer, constructor, "", className);
            writer.write(String.format("{%n"));

            writer.write(
                    indent(
                            String.format(
                                    "super(%s);", Arrays.stream(constructor.getParameters())
                                            .map(Parameter::getName)
                                            .collect(Collectors.joining(","))
                            ), 2
                    ));
            // :NOTE: не самый удачный стиль для форматирования. Вообще форматиовать такое сложно, может, стоит предварительно сохранить код в строку, а потом добавить отступ
            writer.write(indent(String.format("%n}%n"), 1));
        }

        Comparator<Method> downcastComparator = (Method m1, Method m2) -> {
            if (m1.getReturnType().equals(m2.getReturnType())) {
                return 0;
            }
            return m1.getReturnType().isAssignableFrom(m2.getReturnType()) ? 1 : -1;
        };

        List<Method> methodsToWrite = new ArrayList<>();
        Stream.concat(Arrays.stream(token.getMethods())
                                .filter(m -> m.getDeclaringClass().isInterface()),
                        Stream.<Class<?>>iterate(token, Objects::nonNull, Class::getSuperclass)
                                .flatMap(curToken -> Arrays.stream(curToken.getDeclaredMethods())))
                .collect(Collectors.groupingBy(MethodInfo::new, TreeMap::new, Collectors.toList()))
                .forEach((key, value) -> value.stream()
                        .takeWhile(m -> !Modifier.isFinal(m.getModifiers()))
                        .filter(m -> Modifier.isAbstract(m.getModifiers()) && !Modifier.isPrivate(m.getModifiers()))
                        .min(downcastComparator)
                        .ifPresent(methodsToWrite::add));

        for (Method m : methodsToWrite) {
            if (Modifier.isPrivate(m.getReturnType().getModifiers())) {
                throw new ImplerException("Private return type");
            } else if (Arrays.stream(m.getParameters()).anyMatch(p -> Modifier.isPrivate(p.getType().getModifiers()))) {
                throw new ImplerException("Private argument");
            }
            writeMethod(writer, m);
        }
        writer.write("}");
    }

    /**
     * Main function that writes implementation of your class.
     * Creates file with name "YouClassName{Impl}.java" which contains implementation of your class.
     * If user give -jar option, then creating jar file "YouClassName{Impl}.jar".
     *
     * @param args Parameters in format "-jar className jarFile.jar" for jar implementing
     *             or "className path/to/implementation" for simple implementation without jar
     * @throws ImplerException If it's impossible to extend/implement your class/file:
     *                         1) Your arguments are in wrong format
     *                         2) It's impossible to find class, that you want to be implemented
     *                         3) Your "path/to/implementation" is invalid
     *                         4) Other exception that can be thrown by implementJar() or implement()
     * @see Implementor#implement(Class, Path)
     * @see Implementor#implementJar(Class, Path)
     * @since 21
     */
    public static void main(String[] args) throws ImplerException {
        // :NOTE: main, который кидае исключения -- не очень user friendly.
        // :NOTE: вероятно, если вы хотите аргументы обрабатывать с бросанием исключений, то стоит это вынести в отдельный метод, а main ловить искобчение и присать егго сообщение в stderr
        if (args == null || args.length < 2 || args.length > 3 || Arrays.stream(args).allMatch(Objects::isNull)) {
            throw new ImplerException("Usage: [-jar] {className} {pathToImplementation}");
        }
        Class<?> token;
        Path path;
        try {
            token = Class.forName(args[args.length - 2]);
        } catch (ClassNotFoundException e) {
            throw new ImplerException(String.format("Couldn't find class %s: %s%n", args[args.length - 2], e.getMessage()));
        }
        try {
            path = Path.of(args[args.length - 1]);
        } catch (InvalidPathException e) {
            throw new ImplerException(String.format("Invalid path: %s. %s", args[args.length - 2], e.getMessage()));
        }

        if (args.length == 2) {
            new Implementor().implement(token, path);
        } else {
            if (!args[0].equals("-jar")) {
                throw new ImplerException("Usage: [-jar] {className} {pathToImplementation}");
            }
            new Implementor().implementJar(token, path);
        }
    }

    /**
     * Checks if it is possible to implement class {@code token}
     *
     * @param token Class, that is to be checked
     * @throws ImplerException If it is not possible, that is, if one of the following conditions are tru:
     *                         1) {@code Record.class.equals(token) || Enum.class.equals(token)}
     *                         2) {@code token.isEnum()}
     *                         3) {@code Modifier.isPrivate(token.getModifiers())}
     *                         4) {@code token.isPrimitive()}
     *                         5) {@code Modifier.isFinal(token.getModifiers())}
     * @since 21
     */
    private void checkIfCanImplement(Class<?> token) throws ImplerException {
        // :NOTE: если написать через else if, будет чуть короче
        if (Record.class.equals(token) || Enum.class.equals(token)) {
            throw new ImplerException(String.format("Not implementing class %s", token.getSimpleName()));
        }
        if (token.isEnum()) {
            throw new ImplerException("Not implementing enums");
        }
        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Not implementing private members");
        }
        if (token.isPrimitive()) {
            throw new ImplerException("Not implementing primitive types");
        }
        if (Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Not implementing final members");
        }
    }

    /**
     * Creates implementation for class/interface token.
     * Creates file in root with name {@code String.format("%sImpl", token.getSimpleName())}, which contains implementation of {@code token}.
     *
     * @param token type token to create implementation for.
     * @param root  root directory, where implementation will be stored.
     * @throws ImplerException If it's impossible to implement token:
     *                         1) token.equals(Record.class) or token.equals(Enum.class)
     *                         2) token is not interface or class
     *                         3) token is Final
     *                         4) token is Private
     *                         5) token is Utility class
     *                         6) If some IOException occurred, like failing to create file, or failing to write in a file
     * @see Impler#implement(Class, Path)
     * @since 21
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        checkIfCanImplement(token);

        Path outputFile = getOutputFilePath(token, root, "java");
        root = resolvePackages(token, root);

        try {
            Files.createDirectories(root);
        } catch (
                IOException ignored) { // потому что может не быть прав на создание папки, но сама папка может существовать
        }

        try {
            Files.createFile(outputFile);
        } catch (FileAlreadyExistsException e) {
            System.err.printf("WARNING: File %s already exists. Content will be overwritten%n", outputFile);
        } catch (IOException e) {
            throw new ImplerException("Couldn't create .java file: " + e.getMessage());
        }

        try (ToUnicodeWriter writer = new ToUnicodeWriter(Files.newBufferedWriter(outputFile))) {
            implementClass(writer, token);
        } catch (IOException e) {
            throw new ImplerException("Couldn't write to .java file: " + e.getMessage());
        }
    }

    /**
     * Returns path to BaseImplementor class.
     *
     * @return path to BaseImplementor class
     * @since 21
     */
    private static String getClassPath() {
        try {
            return Path.of(BaseImplementorTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Compiles ".java" files.
     *
     * @param root    Path where files are stored
     * @param files   Names of files
     * @param charset Charset that is used to compile files
     * @since 21
     */
    private static void compile(final Path root, final List<String> files, final Charset charset) {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final String classpath = root + File.pathSeparator + getClassPath();
        // :NOTE: зачем создавать стрим, а потом собиорать в массив
        // :NOTE: почему бы не сразу воспользоваться конструктором
        final String[] args = Stream.concat(files.stream(), Stream.of("-cp", classpath, "-encoding", charset.name())).toArray(String[]::new);
        compiler.run(null, null, null, args);
    }

    /**
     * Adds directories to root.
     * Transforms {@code token.getPackage()} to path, and then concatenates root with this path
     *
     * @param token Class, whose packages we will use to resolve root
     * @param root  Directory, that we want to resolve
     * @return Resolved directory root
     * @since 21
     */
    private Path resolvePackages(Class<?> token, Path root) {
        String packageDirectories = token.getPackage().getName().replace('.', File.separatorChar);
        return root.resolve(packageDirectories);
    }

    /**
     * Resolves packages on root, then adds file with implementation to the end of the path
     *
     * @param token     Class, that we're implementing
     * @param root      Directory, where implementation file is stored
     * @param extension Extension that will be added to implementation file
     * @return Path to implementation file
     * @since 21
     */
    private Path getOutputFilePath(Class<?> token, Path root, String extension) {
        return resolvePackages(token, root).resolve(String.format("%sImpl.%s", token.getSimpleName(), extension));
    }

    /**
     * Creates jar file, containing implementation of class {@code token}.
     * Jar file located in {@code jarFile}.
     *
     * @param token   type token to create implementation for.
     * @param jarFile target <var>.jar</var> file.
     * @throws ImplerException If implement(token, root) throws ImplerException
     * @see JarImpler#implementJar(Class, Path)
     * @since 21
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path root = jarFile.getParent();

        Path outputBinaryFile = getOutputFilePath(token, root, "class");

        implement(token, root);
        compile(root, List.of(getOutputFilePath(token, root, "java").toString()), StandardCharsets.UTF_8);

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "info.kgeorgiy.ja.matveev.implementor.Implementor");

        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            String res = getOutputFilePath(token, Path.of(""), "class")
                    .toString()
                    .replace(File.separatorChar, '/');
            JarEntry jarEntry = new JarEntry(res);
            out.putNextEntry(jarEntry);
            Files.copy(outputBinaryFile, out);
        } catch (IOException e) {
            throw new ImplerException("Couldn't write to jar");
        }
    }
}
