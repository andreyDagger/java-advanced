package info.kgeorgiy.ja.matveev.implementor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * Class that describes the most valuable things of method, that can be used do distinguish two methods.
 *
 * @author Andrey Matveev
 * @version 21
 */
public class MethodInfo implements Comparable<MethodInfo> {
    /**
     * Name of the function
     *
     * @since 21
     */
    private final String name;
    /**
     * Array of types of function arguments
     *
     * @since 21
     */
    private final String[] argTypes;

    /**
     * MethodInfo constructor from {@link Method}
     *
     * @param method Method for which we're creating description
     * @since 21
     */
    public MethodInfo(Method method) {
        name = method.getName();
        argTypes = Arrays.stream(method.getParameters())
                .map(m -> m.getType().getSimpleName())
                .toArray(String[]::new);
    }

    /**
     * Compares two MethodInfo objects.
     * Firstly, compares method names, then lexicographically compares their parameters arrays.
     *
     * @param other The object to be compared.
     * @return Compare result
     * @since 21
     */
    @Override
    public int compareTo(MethodInfo other) {
        if (Objects.equals(name, other.name)) {
            for (int i = 0; i < argTypes.length; ++i) {
                if (i >= other.argTypes.length) {
                    return 1;
                }
                int compareRes = argTypes[i].compareTo(other.argTypes[i]);
                if (compareRes != 0) {
                    return compareRes;
                }
            }
            if (argTypes.length == other.argTypes.length) {
                return 0;
            } else {
                return -1;
            }
        }
        return name.compareTo(other.name);
    }
}
