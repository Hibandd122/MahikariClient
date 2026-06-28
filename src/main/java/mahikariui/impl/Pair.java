/*
 * Decompiled with CFR 0.152.
 */
package mahikariui.impl;

import java.util.Objects;

public class Pair<U, V> {
    private U first;
    private V second;

    public Pair(U first, V second) {
        this.put(first, second);
    }

    public Pair(Pair<U, V> other) {
        this(other.getFirst(), other.getSecond());
    }

    public Pair() {
    }

    public Pair<U, V> copy() {
        return new Pair<U, V>(this);
    }

    public U getFirst() {
        return this.first;
    }

    public void setFirst(U first) {
        this.first = first;
    }

    public Pair<U, V> putFirst(U first) {
        this.setFirst(first);
        return this;
    }

    public V getSecond() {
        return this.second;
    }

    public void setSecond(V second) {
        this.second = second;
    }

    public void put(U first, V second) {
        this.setFirst(first);
        this.setSecond(second);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Pair)) {
            return false;
        }
        Pair other = (Pair)obj;
        return Objects.equals(other.getFirst(), this.getFirst()) && Objects.equals(other.getSecond(), this.getSecond());
    }

    public String toString() {
        return "Pair[key=" + (this.getFirst() != null ? this.getFirst().toString() : "N/A") + "; value=" + (this.getSecond() != null ? this.getSecond().toString() : "N/A") + "]";
    }

    public int hashCode() {
        return Objects.hash(this.getFirst(), this.getSecond());
    }
}

