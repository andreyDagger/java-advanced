package info.kgeorgiy.ja.matveev.iterative;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Class, that allows to calculate function on some array of values in parallel.
 *
 * @author Andrey Matveev
 * @version 21
 * @see ParallelMapper
 * @since 21
 */
public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> threadList = new ArrayList<>();
    private final SynchronizedQueue<TaskWrapper> tasksQueue = new SynchronizedQueue<>();

    /**
     * Constructor, that constructs {@code ParallelMapperImpl} with {@code threads} number of threads
     *
     * @param threads Number of threads, that will be used, when calculating {@link ParallelMapper#map(Function, List)}
     */
    public ParallelMapperImpl(final int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("\"threads\" must be positive number");
        }
        final Runnable taskHandler = () -> {
            try {
                while (!Thread.interrupted()) {
                    tasksQueue.poll().run();
                }
            } catch (final InterruptedException ignored) {
            }
        };
        IntStream.range(0, threads).forEach(i -> {
            final Thread thread = new Thread(taskHandler);
            threadList.add(thread);
            thread.start();
        });
    }

    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args) throws InterruptedException {
        final ResultList<R> results = new ResultList<>(args.size());

        tasksQueue.addAll(IntStream.range(0, args.size())
                .mapToObj(i -> new TaskWrapper(() -> {
                    try {
                        results.set(i, f.apply(args.get(i)));
                    } catch (final RuntimeException e) {
                        results.addException(e);
                    }
                }, results))
                .toList());

        return results.getResults();
    }

    @Override
    public void close() {
        for (final Thread thread : threadList) {
            thread.interrupt();
            while (true) {
                try {
                    thread.join();
                } catch (final InterruptedException ignored) {
                    // :NOTE: restore at the end of for loop interrupted flag for current thread (that called close())
                    thread.interrupt();
                    continue;
                }
                break;
            }
        }
        tasksQueue.stream().forEach(task -> task.results.close());
    }

    private static class SynchronizedQueue<T> {
        private final Queue<T> tasks = new ArrayDeque<>();

        public synchronized void addAll(final Collection<T> collection) {
            collection.forEach(task -> {
                tasks.add(task);
                notify();
            });
        }

        public synchronized T poll() throws InterruptedException {
            while (tasks.isEmpty()) {
                wait();
            }

            return tasks.poll();
        }

        public Stream<T> stream() {
            return tasks.stream();
        }
    }

    // :NOTE: parametrize
    private static class TaskWrapper {
        private final Runnable task;
        private final ResultList<?> results;

        public TaskWrapper(final Runnable task, final ResultList<?> results) {
            this.task = task;
            this.results = results;
        }

        public void run() {
            task.run();
            results.decreaseCounter();
        }
    }

    private static class ResultList<T> {
        private final List<T> list;
        private int counter;
        private volatile boolean isClosed = false;
        private RuntimeException accumulatedExceptions = null;

        public ResultList(final int size) {
            list = new ArrayList<>(Collections.nCopies(size, null));
            counter = size;
        }

        public void close() {
            isClosed = true;
            notifyAll();
        }

        public synchronized List<T> getResults() {
            while (counter > 0 && !isClosed) {
                try {
                    wait();
                } catch (final InterruptedException ignored) {
                }
            }
            if (accumulatedExceptions != null) {
                throw accumulatedExceptions;
            }
            return list;
        }

        public void set(final int index, final T t) {
            list.set(index, t);
        }

        public synchronized void decreaseCounter() {
            --counter;
            if (counter == 0) {
                notify();
            }
        }

        public synchronized void addException(final RuntimeException e) {
            if (accumulatedExceptions == null) {
                accumulatedExceptions = e;
            } else {
                accumulatedExceptions.addSuppressed(e);
            }
        }
    }
}
