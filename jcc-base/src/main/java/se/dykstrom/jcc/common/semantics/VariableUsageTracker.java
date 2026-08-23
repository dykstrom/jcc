package se.dykstrom.jcc.common.semantics;

import se.dykstrom.jcc.common.ast.Node;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Tracks variable declaration and usage for unused variable warnings.
 */
public class VariableUsageTracker {

    private Map<String, Node> declaredVariables = new HashMap<>();
    private Set<String> usedVariables = new HashSet<>();

    private final List<Map<String, Node>> declaredStack = new ArrayList<>();
    private final List<Set<String>> usedStack = new ArrayList<>();

    /**
     * Registers variable declaration.
     */
    public void declare(final String name, final Node node) {
        declaredVariables.put(name, node);
    }

    /**
     * Registers variable usage.
     */
    public void use(String name) {
        usedVariables.add(name);
    }

    /**
     * Checks for unused variables and reports warnings for any that were declared but never used.
     */
    public void check(final BiConsumer<Node, String> warningsReporter) {
        declaredVariables.forEach((name, node) -> {
            if (!usedVariables.contains(name)) {
                warningsReporter.accept(node, "unused variable: " + name);
            }
        });
    }

    /**
     * Checks for unused variables among the given names only, and reports warnings for any that
     * were declared but never used. Used to check a function's own parameters without touching
     * enclosing (global) variables, which are checked at the top level once all usages are known.
     */
    public void check(final Set<String> names, final BiConsumer<Node, String> warningsReporter) {
        declaredVariables.forEach((name, node) -> {
            if (names.contains(name) && !usedVariables.contains(name)) {
                warningsReporter.accept(node, "unused variable: " + name);
            }
        });
    }

    /**
     * Saves the current state of the tracker before parsing a function.
     */
    public void save() {
        declaredStack.addLast(new HashMap<>(declaredVariables));
        usedStack.addLast(new HashSet<>(usedVariables));
    }

    /**
     * Restores the previous state of the tracker after parsing a function.
     */
    public void restore(final Set<String> parameterNames) {
        final var globalVariablesUsedInFunction = new HashSet<>(usedVariables);
        globalVariablesUsedInFunction.removeAll(parameterNames);

        declaredVariables = declaredStack.removeLast();
        usedVariables = usedStack.removeLast();

        usedVariables.addAll(globalVariablesUsedInFunction);
    }
}
