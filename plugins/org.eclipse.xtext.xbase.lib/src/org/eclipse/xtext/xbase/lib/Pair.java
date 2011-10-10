/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xbase.lib;

/**
 * An immutable pair of {@link #getKey() key} and {@link #getValue() value}. A pair is considered to be
 * {@link #equals(Object) equal} to another pair if both the key and the value are equal.
 * 
 * @param <K>
 *            the key-type of the pair.
 * @param <V>
 *            the value-type of the pair.
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public final class Pair<K, V> {

	private final K k;
	private final V v;

	/**
	 * Creates a new instance with the given key and value.
	 * 
	 * @param k
	 *            the key. May be <code>null</code>.
	 * @param v
	 *            the value. May be <code>null</code>.
	 * 
	 */
	public Pair(K k, V v) {
		this.k = k;
		this.v = v;
	}

	/**
	 * Returns the key.
	 * 
	 * @return the key.
	 */
	public K getKey() {
		return k;
	}

	/**
	 * Returns the value.
	 * 
	 * @return the value.
	 */
	public V getValue() {
		return v;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Pair))
			return false;
		Pair<?, ?> e = (Pair<?, ?>) o;
		return ObjectExtensions.operator_equals(k, e.getKey()) && ObjectExtensions.operator_equals(v, e.getValue());
	}

	@Override
	public int hashCode() {
		return ((k == null) ? 0 : k.hashCode()) ^ ((v == null) ? 0 : v.hashCode());
	}

	@Override
	public String toString() {
		return k + "->" + v;
	}

}
