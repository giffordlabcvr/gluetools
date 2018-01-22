/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * A classic Multiton making use of lambdas to delay the create.
 *
 * @param <K> - The type that can create.
 */
public class Multiton {

    /**
     * The keys must be capable of creating their values.
     */
    public interface Creator<V> {
        public Object create();
        public V cast(Object value);
    }

    public static abstract class TypedCreator<A> implements Creator<A> {
        private final Class<A> tclass;
        public TypedCreator(Class<A> tclass) {
            this.tclass = tclass;
        }

        @Override
        public final A cast(Object value) {
            return tclass.cast(value);
        }
    }

    public static final class SuppliedCreator<A> extends TypedCreator<A> {
        private final Supplier<A> supplier;
        public SuppliedCreator(Class<A> tclass, Supplier<A> supplier) {
            super(tclass);
            this.supplier = supplier;
        }

        @Override
        public Object create() {
            return supplier.get();
        }
    }

    /**
     * The storage.
     *
     * Store only Object because they must all be different types.
     */
    private final ConcurrentMap<Creator<?>, Object> multitons = new ConcurrentHashMap<>();

    /**
     * The getter.
     *
     * @param <V> - The type of the value that should be returned.
     * @param key - The unique key behind which the value is to be stored.
     * @return - The value stored (and perhaps created) behind the key.
     */
    public <V, C extends Multiton.Creator<V>> V get(final C key) {
        return key.cast(multitons.computeIfAbsent(key, k -> k.create()));
    }
}