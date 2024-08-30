package info.kgeorgiy.ja.matveev.iterative;

import info.kgeorgiy.java.advanced.iterative.AdvancedIP;
import info.kgeorgiy.java.advanced.iterative.NewListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Class, that allows to do basic iterative things with parallelism.
 *
 * @author Andrey Matveev
 * @version 21
 * @see NewListIP
 * @since 21
 */
public class IterativeParallelism implements AdvancedIP {
    private final ParallelMapper parallelMapper;

    /**
     * Default constructor, constructs with null {@code parallelMapper}
     */
    public IterativeParallelism() {
        this.parallelMapper = null;
    }

    /**
     * Constructs, with usage of ParallelMapper
     *
     * @param parallelMapper Mapper, that wil be used in parallelism
     */
    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    private <T> T maximumImpl(int threads, List<T> values, Comparator<? super T> comparator, int step) throws InterruptedException {
        return reduce(threads, values, values.getFirst(), (a, b) -> comparator.compare(a, b) >= 0 ? a : b, step);
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator, int step) throws InterruptedException {
        return values.isEmpty() ? null : maximumImpl(threads, values, comparator, step);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator, int step) throws InterruptedException {
        return maximum(threads, values, comparator.reversed(), step);
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return mapReduce(threads, values, predicate::test, true, (a, b) -> (a && b), step);
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return !all(threads, values, Predicate.not(predicate), step);
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return mapReduce(threads, values, x -> predicate.test(x) ? 1 : 0, 0, Integer::sum, step);
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator, 1);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return minimum(threads, values, comparator, 1);
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return all(threads, values, predicate, 1);
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return any(threads, values, predicate, 1);
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return count(threads, values, predicate, 1);
    }

    private <T, R, U> R doParallel(int threads, List<? extends T> values, Collector<T, ?, U> innerCollector, Collector<U, ?, R> outerCollector, R defaultValue) throws InterruptedException {
        if (values.isEmpty()) {
            return defaultValue;
        }
        threads = Math.min(threads, values.size());
        int blockSize = values.size() / threads;

        if (parallelMapper == null) {
            List<U> results = new ArrayList<>(Collections.nCopies(threads, null));
            List<Thread> workers = new ArrayList<>();
            int l = 0;
            for (int workerIndex = 0; workerIndex < threads; ++workerIndex) {
                final int r = l + blockSize + (workerIndex < values.size() % threads ? 1 : 0);
                final int finalWorkerIndex = workerIndex;
                final int finalL = l;
                workers.add(new Thread(() -> results.set(finalWorkerIndex, values.subList(finalL, r).stream().collect(innerCollector))));
                workers.get(workerIndex).start();
                l = r;
            }
            InterruptedException e = null;
            for (Thread thread : workers) {
                while (true) {
                    try {
                        thread.join();
                    } catch (InterruptedException exception) {
                        if (e == null) {
                            e = exception;
                        }
                        continue;
                    }
                    break;
                }
            }
            if (e != null) {
                throw e;
            }
            return results.stream().collect(outerCollector);
        } else {
            int l = 0;
            List<List<? extends T>> subLists = new ArrayList<>();
            for (int workerIndex = 0; workerIndex < threads; ++workerIndex) {
                int r = l + blockSize + (workerIndex < values.size() % threads ? 1 : 0);
                subLists.add(values.subList(l, r));
                l = r;
            }

            try {
                List<U> results = parallelMapper.map(s -> s.stream().collect(innerCollector), subLists);
                return results.stream().collect(outerCollector);
            } catch (InterruptedException e) {
                parallelMapper.close();
                throw e;
            }
        }
    }

    @Override
    public String join(int threads, List<?> values, int step) throws InterruptedException {
        return mapReduce(threads, values, Object::toString, "", (a, b) -> (a + b), step);
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return doParallel(threads,
                new KthElementListView<>(values, step),
                Collectors.filtering(predicate, Collectors.toList()),
                Collectors.reducing(new ArrayList<>(), (a, b) -> {
                    a.addAll(b);
                    return a;
                }),
                List.of());
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f, int step) throws InterruptedException {
        return mapReduce(threads, values, x -> new ArrayList<>(Collections.nCopies(1, f.apply(x))), new ArrayList<>(), (a, b) -> {
            var c = new ArrayList<>(a);
            c.addAll(b);
            return c;
        }, step);
    }

    @Override
    public String join(int i, List<?> list) throws InterruptedException {
        return join(i, list, 1);
    }

    @Override
    public <T> List<T> filter(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return filter(i, list, predicate, 1);
    }

    @Override
    public <T, U> List<U> map(int i, List<? extends T> list, Function<? super T, ? extends U> function) throws InterruptedException {
        return map(i, list, function, 1);
    }

    @Override
    public <T> T reduce(int threads, List<T> values, T identity, BinaryOperator<T> operator, int step) throws InterruptedException {
        return mapReduce(threads, values, Function.identity(), identity, operator, step);
    }

    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, R identity, BinaryOperator<R> operator, int step) throws InterruptedException {
        return doParallel(threads,
                new KthElementListView<>(values, step),
                Collectors.reducing(identity, lift, operator),
                Collectors.reducing(identity, operator),
                identity);
    }
}
