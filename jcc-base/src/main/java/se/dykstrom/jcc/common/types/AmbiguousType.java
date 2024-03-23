/*
 * Copyright 2024 Johan Dykström
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.dykstrom.jcc.common.types;

import java.util.Set;

/**
 * Represents a type that cannot be unambiguously parsed without using type inference.
 */
public record AmbiguousType(Set<Type> types) implements Type {

    public AmbiguousType(final Set<Type> types) {
        this.types = Set.copyOf(types);
    }

    public boolean contains(final Type type) {
        return types.stream().anyMatch(t -> t.equals(type));
    }

    @Override
    public String getName() {
        return "One of " + types.size() + " possible";
    }

    @Override
    public String getDefaultValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFormat() {
        throw new UnsupportedOperationException();
    }
}
