package com.firmfreez.currencyedittext.extensions

/**
 * Позволяет вставить строку в указанную позицию текста
 *
 * @param value вставляемая строка
 * @param position индекс, куда необходимо вставить строку
 *
 * @return Если индекс > длины текста, то возвращает исходный текст + вставляемую строку,
 * если индекс < длины текста - возвращает исходный текст, иначе возвращает текст со вставленной строкой
 * в указанный идекс
 */
fun String.insert(value: String, position: Int): String {
    val pos = if (position > this.length) this.length else if (position < 0) return this else position
    return this.substring(0, pos) + value + this.substring(pos)
}