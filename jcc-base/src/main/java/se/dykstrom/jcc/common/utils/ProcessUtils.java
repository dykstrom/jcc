/*
 * Copyright (C) 2016 Johan Dykstrom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.dykstrom.jcc.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Contains static utility methods related to process management.
 *
 * @author Johan Dykstrom
 */
public final class ProcessUtils {

    private ProcessUtils() { }

    /** Timeout for waiting on a process to exit, and for its output-draining thread to finish. */
    private static final long TIMEOUT_SECONDS = 10;

    /** Holds the output captured for each running process, keyed by the process itself. */
    private static final Map<Process, OutputCapture> CAPTURES = new ConcurrentHashMap<>();

    /**
     * Sets up and returns a new process that executes the given {@code command}.
     * Before starting the process, the environment of the process is extended with
     * any environment variables given in {@code addEnv}.
     *
     * @param command The command to execute.
     * @param addEnv  A map of environment variables to set before executing the command.
     */
    public static Process setUpProcess(List<String> command, Map<String, String> addEnv) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.environment().putAll(addEnv);
        return startAndWait(builder);
    }

    /**
     * Sets up and returns a new process that executes the given {@code command},
     * reading its input from {@code inputFile}. Before starting the process, the
     * environment of the process is extended with any environment variables given
     * in {@code addEnv}.
     *
     * @param command   The command to execute.
     * @param inputFile The input file that stdin will be redirected to.
     * @param addEnv    A map of environment variables to set before executing the command.
     */
    public static Process setUpProcess(List<String> command, File inputFile, Map<String, String> addEnv) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true).redirectInput(inputFile);
        builder.environment().putAll(addEnv);
        return startAndWait(builder);
    }

    private static Process startAndWait(ProcessBuilder builder) throws IOException, InterruptedException {
        Process process = builder.start();

        // Drain the process output on a background thread. Otherwise a process that writes more
        // than the OS pipe buffer (~4 KB on Windows) blocks on write and never exits, because
        // nothing reads the pipe until after waitFor returns.
        OutputCapture capture = new OutputCapture(process);
        CAPTURES.put(process, capture);
        capture.start();

        // Wait for the process to start and then end
        process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Return the already ended process
        return process;
    }

    /**
     * Tears down the given process.
     */
    public static void tearDownProcess(Process process) {
        process.destroy();
        CAPTURES.remove(process);
    }

    /**
     * Reads all output that is available from the given {@code process}, and returns this as a single string.
     *
     * @param process The process to read from.
     * @return The process output.
     */
    public static String readOutput(Process process) {
        OutputCapture capture = CAPTURES.get(process);
        return (capture != null) ? capture.getOutput() : "";
    }

    /**
     * Reads a process's combined stdout/stderr to EOF on a daemon thread, so the process is never
     * blocked by a full pipe buffer.
     */
    private static final class OutputCapture {

        private final Process process;
        private final StringBuilder builder = new StringBuilder();
        private final Thread thread;

        OutputCapture(Process process) {
            this.process = process;
            this.thread = new Thread(this::drain, "process-output-capture");
            this.thread.setDaemon(true);
        }

        void start() {
            thread.start();
        }

        private void drain() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (builder) {
                        builder.append(line).append("\n");
                    }
                }
            } catch (IOException e) {
                synchronized (builder) {
                    builder.append(e.getMessage()).append("\n");
                }
            }
        }

        String getOutput() {
            try {
                thread.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            synchronized (builder) {
                return builder.toString();
            }
        }
    }
}
