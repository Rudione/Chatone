package io.rudione.chatone.util

actual fun <K, V> createMutableMapWithCapacity(capacity: Int): MutableMap<K, V> = mutableMapOf()
actual fun <T> createMutableListWithCapacity(capacity: Int): MutableList<T> = mutableListOf()
actual fun <T> createMutableSetWithCapacity(capacity: Int): MutableSet<T> = mutableSetOf()