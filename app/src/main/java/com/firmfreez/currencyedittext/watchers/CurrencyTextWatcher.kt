package com.firmfreez.currencyedittext.watchers

import android.widget.EditText
import com.firmfreez.currencyedittext.extensions.insert
import java.lang.ref.WeakReference
import java.math.BigDecimal

/**
 * TextWatcher для поля ввода суммы
 * Проверяет и форматирует текст
 *
 * @param editText слабая ссылка на родительский EditText
 * @param onValueChanged колбэк изменения значения
 */
class CurrencyTextWatcher(
    private val editText: WeakReference<EditText>,
    private val onValueChanged: ((BigDecimal?) -> Unit)? = null
): EasyTextWatcher() {

    /**
     * Метод вызывается при изменении текста
     */
    override fun onTextModified(newPartOfText: String?, newText: String?, oldText: String?, editPosition: Int?) {

        // Подсчет количества пробелов (разделителей)
        val spaceCount = newText?.count { c -> c == ' ' }

        // Расчет позиции точки (в тексте без разделителей)
        val dotPos = newText?.let {
            val pos = it.indexOf(".")
            if (pos == -1) pos else pos - (spaceCount ?: 0)
        }?: -1

        // Расчет позиции курсора (в тексте без разделителей)
        var resultEditPosition = editPosition?.let {
            val text = newText?.substring(0, it)
            val curSpaceCount = text?.count { c -> c == ' ' } ?: 0
            it - curSpaceCount
        } ?: 0

        // Новый текст без разделителей
        val newTextWithoutSpace = newText?.replace(" ", "")

        // Измененная часть в новом тексте без разделителей
        val newPartOfTextWithoutSpace = newPartOfText?.replace(" ", "")

        // Старый текст без разделителей
        val oldTextWithoutSpace = oldText?.replace(" ", "")

        // Расчет текста до точки
        var textBeforeDot = if (dotPos == -1) {
            newTextWithoutSpace ?: ""
        } else {
            newTextWithoutSpace?.substring(0, dotPos) ?: ""
        }

        // Расчет текста после точки
        var textAfterDot = if (dotPos == -1) {
            null
        } else {
            newTextWithoutSpace?.substring(dotPos + 1, newTextWithoutSpace.length)?: ""
        }

        // Если поле ввода состоит только из 0 - заменяем его введенным символом
        if (!newPartOfTextWithoutSpace.isNullOrEmpty() && oldTextWithoutSpace == "0" && textAfterDot == null && dotPos == -1 && resultEditPosition == 1) {
            setText(resultText = newPartOfTextWithoutSpace, resultEditPosition = resultEditPosition)
            return
        }

        // Убираем все незначащие нули в начале (если они образовались)
        while (textBeforeDot.startsWith("0")) {
            textBeforeDot = textBeforeDot.removePrefix("0")
            if (resultEditPosition > 0) resultEditPosition--
        }

        // Если текст до точки пустой - вставляем туда ноль
        if (textBeforeDot.isEmpty()) {
            textBeforeDot = "0"
            if (editPosition != 0) resultEditPosition++
        }

        // Если текст после точки пустой - возвращаем пустую строку, иначе строку вида .XXX
        textAfterDot = if (textAfterDot != null) {
            ".$textAfterDot"
        } else {
            ""
        }

        // Собираем результирующую строку
        val resultText = textBeforeDot + textAfterDot

        // Устанавливаем текст
        setText(resultText = resultText, resultEditPosition = resultEditPosition)
    }

    /**
     * @return [EditText]? развоачивает слабую ссылку для удобства.
     */
    private fun editText(): EditText? {
        return editText.get()
    }

    /**
     * Установка текста в EditText
     * @param resultText устанавливаемый текст
     * @param resultEditPosition позиция курсора
     */
    private fun setText(resultText: String?, resultEditPosition: Int?) {
        // Устанавливаем разделители в текст
        val (text, position) = calculateSpacing(
            resultText = resultText,
            resultEditPosition = resultEditPosition
        )

        // Устанавливаем текст
        editText()?.setText((text as? String?) ?: "")

        // Устанавливаем курсор на нужную позицию
        editText()?.setSelection((position as? Int?) ?: 0)

        // Возвращаем колбэк
        onValueChanged?.invoke(resultText?.toBigDecimalOrNull())
    }

    /**
     * Подсчет и установка разделителей в виде пробелов в текст
     * @param resultText Текст в который необходимо установить разделители
     * @param resultEditPosition Позиция курсора, которую необходимо сохранить
     *
     * @return [Array] Массив содержащий 2 значения [String] - результирующий текст, [Int] - позиция курсора
     */
    private fun calculateSpacing(resultText: String?, resultEditPosition: Int?): Array<Any?> {
        // Переменная для расчета позиции
        var resultPosition = resultEditPosition ?: 0

        // Расчет индекса точки в тексте
        val dotPos = resultText?.indexOf(".") ?: -1

        // Расчет текста до точки
        var textBeforeDot = if (dotPos == -1) {
            resultText ?: ""
        } else {
            resultText?.substring(0, dotPos) ?: ""
        }

        // Расчет текста после точки
        var textAfterDot = if (dotPos == -1) {
            null
        } else {
            resultText?.substring(dotPos + 1, resultText.length)?: ""
        }

        // Расчет количества отступов
        val spaceCount = textBeforeDot.length / 3

        // Индекс последнего символа
        var index = textBeforeDot.length

        // Перебираем все пробелы и устанавливаем их в результирующий текст
        // паралельно сдвигая курсор
        for (i in 1 until spaceCount + 1) {
            index -= 3
            if (index > 0) {
                if (index < resultPosition) {
                    resultPosition++
                }
                textBeforeDot = textBeforeDot.insert(" ", index)
            }
        }

        // Окончательный расчет текста после точки
        textAfterDot = if (textAfterDot != null) {
            ".$textAfterDot"
        } else {
            ""
        }

        // Подсчет результирующей строки
        val result = textBeforeDot + textAfterDot

        return arrayOf(result, resultPosition)
    }
}