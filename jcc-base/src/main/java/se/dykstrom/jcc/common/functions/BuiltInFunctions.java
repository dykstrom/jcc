/*
 * Copyright (C) 2018 Johan Dykstrom
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

package se.dykstrom.jcc.common.functions;

/**
 * Contains a number of common built-in functions.
 *
 * @author Johan Dykstrom
 */
public final class BuiltInFunctions {

    // Memory management
    public static final AssemblyFunction FUN_MEMORY_MARK     = new MemoryMarkFunction();
    public static final AssemblyFunction FUN_MEMORY_SWEEP    = new MemorySweepFunction();
    public static final AssemblyFunction FUN_MEMORY_REGISTER = new MemoryRegisterFunction();

    private BuiltInFunctions() { }
}
