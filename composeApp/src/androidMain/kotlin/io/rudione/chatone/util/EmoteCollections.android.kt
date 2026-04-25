package io.rudione.chatone.util

import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

actual fun <K, V> createMutableMapWithCapacity(capacity: Int): MutableMap<K, V> =
    HashMap<K, V>(capacity)

actual fun <T> createMutableListWithCapacity(capacity: Int): MutableList<T> =
    ArrayList<T>(capacity)

actual fun <T> createMutableSetWithCapacity(capacity: Int): MutableSet<T> =
    HashSet<T>(capacity)