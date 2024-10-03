/*
 * Copyright (C) 2024 Johan Dykstrom
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

package se.dykstrom.jcc.llvm;

public enum LlvmOperator {

    ADD, // Integer add
    ALLOCA, // Allocate stack memory
    AND, // Bitwise AND
    BR, // Branch
    CALL, // Call function
    DECLARE, // Declare function prototype
    DEFINE, // Define function
    FADD, // Floating point add
    FCMP, // Floating point compare
    FDIV, // Floating point division
    FMUL, // Floating point multiply
    FNEG, // Floating point negate
    FSUB, // Floating point subtract
    ICMP, // Integer compare
    LOAD, // Load
    MUL, // Integer multiply
    OR, // Bitwise OR
    PHI, // Phi function
    RET, // Return from function
    SEXT, // Sign-extend
    SDIV, // Signed division
    SITOFP, // Signed integer to floating point
    STORE, // Store
    SUB, // Integer subtract
    TRUNC, // Truncate
    XOR, // Bitwise XOR
    ZEXT; // Zero-extend

    public String toText() {
        return name().toLowerCase();
    }
}
