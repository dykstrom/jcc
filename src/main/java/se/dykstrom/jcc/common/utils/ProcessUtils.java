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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Contains static utility methods related to process management.
 *
 * @author Johan Dykstrom
 */
public final class ProcessUtils {

    private ProcessUtils() { }

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
        Process process = builder.start();

        // Wait for the process to start and then end
        process.waitFor(10, TimeUnit.SECONDS);

        // Return the already ended process
        return process;
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
        Process process = builder.start();

        // Wait for the process to start and then end
        process.waitFor(10, TimeUnit.SECONDS);

        // Return the already ended process
        return process;
    }

    /**
     * Tears down the given process.
     */
    public static void tearDownProcess(Process process) {
        process.destroy();
    }

    /**
     * Reads all output that is available from the given {@code process}, and returns this as a single string.
     *
     * @param process The process to read from.
     * @return The process output.
     */
    public static String readOutput(Process process) {
        StringBuilder builder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.ready()) {
                String str = reader.readLine();
                builder.append(str).append("\n");
            }
        } catch (IOException e) {
            builder.append(e.getMessage()).append("\n");
        }

        return builder.toString();
    }
}
