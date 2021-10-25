package com.firmfreez.currencyedittext.inputFilters

import android.text.InputFilter
import android.text.Spanned

/**
 * @author Леон Алексанянц (FirmFreez) 25.10.21
 *
 * Фильтр ввода суммы
 * Допускается число вида: 1 234 567.89
 *
 * @param digitsAfterDot рарешенное максимальное количество символов после точки
 */
class CurrencyInputFilter(
    var digitsAfterDot: Int? = null
): InputFilter {
    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence {
        // Заменяем запятую на точку
        val newSource = source.toString().replace(",", ".")

        // Регулярное выражение числа до точки
        val integerPartRule = "(\\ *?\\d*)*"

        // Регулярное выражение числа после точки (включая точку)
        val decimalPartRule = digitsAfterDot?.let {
            if (it == 0) "" else "(?:\\.\\d{0,$it}?)?"
        }?: "(?:\\.\\d*?)?"

        // Итоговое регулярное выражение
        val rule = "^$integerPartRule$decimalPartRule$".toRegex()

        // Получаем измененный текст
        val newDest = dest?.replaceRange(dstart, dend, newSource)

        // Если измененный текст соответствует регулярному выражению - пропускаем
        if (newDest.toString().matches(rule)) {
            return newSource
        }
        // Иначе запрещаем
        return ""
    }
}