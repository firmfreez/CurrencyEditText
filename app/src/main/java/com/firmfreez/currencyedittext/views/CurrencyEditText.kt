package com.firmfreez.currencyedittext.views

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Canvas
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.alpha
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.firmfreez.currencyedittext.R
import com.firmfreez.currencyedittext.inputFilters.CurrencyInputFilter
import com.firmfreez.currencyedittext.watchers.CurrencyTextWatcher
import java.io.IOException
import java.lang.ref.WeakReference
import java.math.BigDecimal

/**
 * @author Леон Алексанянц (FirmFreez) 25.10.21
 *
 * EditText для ввода суммы
 *
 * @property minValue Минимально допустимая сумма (если null - то нет ограничения)
 * @property maxValue Максимально допустимая сумма (если null - то нет ограничения)
 * @property startValue Стартовое значение (По умолчанию 0)
 * @property currencyType Тип валюты - задается значением из [CurrencyType]
 * @property digitsAfterDot Количество символов после точки (если null - то нет ограничения)
 * @property onValueChanged задается функцией [setOnValueChanged] и возвращает новое значение + состояние [State]
 */
class CurrencyEditText: AppCompatEditText {
    // Множитель для перевода из пикселей в dp
    private var multi = resources.displayMetrics.density

    // Обработчик событий изменения значений
    private var onValueChanged: OnValueChanged? = null

    // Лайвдата со значениями
    private val _liveData = MutableLiveData<BigDecimal?>().apply { postValue(startValue) }
    val liveData: LiveData<BigDecimal?> = _liveData

    // Стартовое значение
    var startValue: BigDecimal = BigDecimal.valueOf(0)

    // Отступ от текста до значка валюты
    var currencySpacing = DEFAULT_CURRENCY_SPACING * multi
        set(value) {
            field = value
            invalidate()
        }

    // Минимально-допустимое значение поля (null, если нет ограничений)
    var minValue: BigDecimal? = null
        set(value) {
            field = value
            if (value != null) {
                maxValue?.let { max ->
                    if (value > max) throw IOException("Минимальное значение ($value) больше максимального ($max)")
                }

                val curValue = getValue()
                if (curValue != null && curValue < value) {
                    setValue(value)
                }
            }
        }

    // Максимально-допустимое значение поля (null, если нет ограничений)
    var maxValue: BigDecimal? = null
        set(value) {
            field = value
            if (value != null) {
                minValue?.let { min ->
                    if (value < min) throw IOException("Максимальное значение ($value) меньше минимального ($min)")
                }

                val curValue = getValue()
                if (curValue != null && curValue > value) {
                    setValue(value)
                }
            }
        }

    // Тип валюты
    var currencyType: CurrencyType = CurrencyType.RUR
        set(value) {
            field = value
            invalidate()
        }

    // Количество символов после запятой (null - если нет ограничений)
    var digitsAfterDot: Int? = null
        set(value) {
            field = value
            setFilter(value)
        }

    /**
     * Конструкторы
     */
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttrs: Int): super(context, attrs, defStyleAttrs) {
        init(context, attrs)
    }

    /**
     * Основная функция инициализации
     * В ней:
     * - Применяются аттирбуты из XML
     * - Настраивается Input (для фильтрации ввода)
     * - Настраивается TextChangeListener (В котором происходит основное форматирование)
     * - Настраивается KeyListener (Для отключения фокуса при нажатии на клавишу Enter)
     * - Устанавливается первоначальное значение
     */
    private fun init(context: Context, attrs: AttributeSet?) {
        obtainStyleAttrs(context, attrs)
        configInput()
        configTextChangeListener()
        configKeyListener()
        configFocusChange()

        setValue(prepareToShow(startValue))
    }

    /**
     * Применяет аттрибуты из XML
     */
    private fun obtainStyleAttrs(context: Context, attrs: AttributeSet?) {
        if (attrs != null) {
            val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.CurrencyEditText, 0, 0)
            currencyType = when (typedArray.getInt(R.styleable.CurrencyEditText_currencyType, 0)) {
                1 -> {
                    CurrencyType.EUR
                }
                2 -> {
                    CurrencyType.USD
                }
                else -> {
                    CurrencyType.RUR
                }
            }
            currencySpacing = typedArray.getDimension(R.styleable.CurrencyEditText_currencyTypeSymbolSpacing, currencySpacing)
            if (typedArray.hasValue(R.styleable.CurrencyEditText_currencyStartValue)) {
                startValue = typedArray.getFloat(
                    R.styleable.CurrencyEditText_currencyStartValue,
                    startValue.toFloat()
                ).toBigDecimal()
            }
            if (typedArray.hasValue(R.styleable.CurrencyEditText_currencyMinValue)) {
                minValue = typedArray.getFloat(
                    R.styleable.CurrencyEditText_currencyMinValue,
                    minValue?.toFloat() ?: 0F
                ).toBigDecimal()
            }
            if (typedArray.hasValue(R.styleable.CurrencyEditText_currencyMaxValue)) {
                maxValue = typedArray.getFloat(
                    R.styleable.CurrencyEditText_currencyMaxValue,
                    maxValue?.toFloat() ?: 0F
                ).toBigDecimal()
            }
            if (typedArray.hasValue(R.styleable.CurrencyEditText_currencyDigitsAfterDot)) {
                digitsAfterDot = typedArray.getInt(
                    R.styleable.CurrencyEditText_currencyDigitsAfterDot,
                    digitsAfterDot ?: 0
                )
            }
        }
    }

    /**
     * Настройка ввода (Разрешается вводить только цифры и точку)
     * Так же устанавливается фильтр ввода
     */
    private fun configInput() {
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        setFilter(digitsAfterDot)
    }

    /**
     * Устанавливается обработчик изменения текста
     * В нем проверяется и форматируется введенный текст, после сравнивается с минимальным
     * и максимальным значением и отправляется колбэк с новым значением и состоянием
     */
    private fun configTextChangeListener() {
        addTextChangedListener(CurrencyTextWatcher(WeakReference(this)) { newValue ->
            var state: State = State.OK

            minValue?.let { min ->
                if (newValue ?: BigDecimal(0.0) < min) {
                    state = State.BAD_MIN_VALUE
                }
            }

            maxValue?.let { max ->
                if (newValue ?: BigDecimal(0.0) > max) {
                    state = State.BAD_MAX_VALUE
                }
            }

            if (hasFocus()) {
                // Если был ввод от пользователя
                onValueChanged?.onValueChanged(newValue, state)
                _liveData.postValue(newValue)
            } else {
                // Если текст был вставлен или установлен через setText
                checkBeforeApplying()
            }
        })
    }

    /**
     * Проверка, что был нажат Enter, и скрытие клавиатуры при нажатии
     * Так же убирается фокус с поля
     */
    private fun configKeyListener() {
        setOnKeyListener { _, i, _ ->
            if (i == KeyEvent.KEYCODE_ENTER) {
                hideKeyboard()
                return@setOnKeyListener true
            }
            false
        }
    }

    /**
     * При снятии фокуса с поля - форматируем текст
     */
    private fun configFocusChange() {
        setOnFocusChangeListener { _, b ->
            if (!b) {
                checkBeforeApplying()
            }
        }
    }

    /**
     * Окончательное форматирование текста, проверка на минимальное и максимальное значение
     * А так же дописывание незначащих нулей
     * Метод возвращает в колбэк финальное значение со статусом Apply
     */
    private fun checkBeforeApplying() {
        var value = getValue() ?: BigDecimal.valueOf(0)
        minValue?.let { min ->
            if (value < min) {
                value = min
            }
        }
        maxValue?.let { max ->
            if (value > max) {
                value = max
            }
        }

        val resultValue = prepareToShow(value)

        if (resultValue != getValue() ?: BigDecimal.valueOf(0)) {
            setValue(resultValue)
        }

        onValueChanged?.onValueChanged(resultValue, State.APPLY)
        _liveData.postValue(resultValue)
    }

    /**
     * Подгоняет число BigDecimal под необходимую точность
     * (удаляет лишние цифры после запятой, или дописывает нули)
     * И возвращает результат
     *
     * @param value сумма
     * @return [BigDecimal] Отформатированная сумма
     */
    private fun prepareToShow(value: BigDecimal): BigDecimal {
        val strValue = adjustToAccuracy(value).toString()
        val endValue = getInsignificantZeros(strValue)
        return  (strValue + endValue).toBigDecimalOrNull() ?: BigDecimal.valueOf(0)
    }

    /**
     * Метод удаляет лишние цифры если точность числа больше заданой
     * @param value сумма
     * @return [BigDecimal] сумма подогнанная под точность
     */
    private fun adjustToAccuracy(value: BigDecimal?): BigDecimal {
        var strValue = value?.toString() ?: ""
        val dotPos = strValue.indexOf(".")
        if (!strValue.isEmpty() && dotPos != -1) {
            val textAfterDot = strValue.substring(dotPos + 1)
            if (textAfterDot.length > digitsAfterDot ?: textAfterDot.length) {
                strValue = strValue.removeRange(strValue.length - textAfterDot.length + (digitsAfterDot ?: 0), strValue.length)
            }
        }

        return strValue.toBigDecimalOrNull() ?: BigDecimal.valueOf(0)
    }

    /**
     * Метод дописывает нули, если точность числа ниже заданой
     * @param rawValue строковое значение суммы
     * @param [String] подогнанное под точность незначащими нулями строковое значение суммы
     */
    private fun getInsignificantZeros(rawValue: String): String {
        val rawValueWithoutSpace = rawValue.replace(" ", "")
        val dotPos = rawValueWithoutSpace.indexOf(".")
        val endValue = digitsAfterDot?.let {
            if (it != 0) {
                return@let if (dotPos == -1) {
                    "." + "0".repeat(it)
                } else {
                    val textAfterDot = rawValueWithoutSpace.substring(dotPos + 1)
                    val zerosCount = it - textAfterDot.length
                    if (zerosCount > 0) {
                        "0".repeat(it - textAfterDot.length)
                    } else ""
                }
            }
            ""
        }
        return endValue ?: ""
    }

    /**
     * Метод снимает фокус с поля и скрывает клавиатуру
     */
    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(windowToken, 0)
        isFocusable = false
        isFocusableInTouchMode = true
    }


    /**
     * Метод реагирует на вставку текста (чтобы заменить старый на новый)
     * @param id Идентификатор выбранной кнопки
     * @return [Boolean] обработка значения вниз по потоку
     */
    override fun onTextContextMenuItem(id: Int): Boolean {
        val consumed = super.onTextContextMenuItem(id)
        when (id) {
            android.R.id.paste -> {
                val lastCopiedData = getLatestClipboardData()
                if (!lastCopiedData.isNullOrEmpty()) {
                    lastCopiedData.replace(",", ".").replace(" ", "").toBigDecimalOrNull() ?.let {
                        val beautyText = prepareToShow(it).toString()
                        setText(beautyText)
                        setSelection(beautyText.length)
                    }
                }
            }
        }
        return consumed
    }

    /**
     * @return [String] последнее скопированное значение с буфера обмена
     *
     */
    private fun getLatestClipboardData(): String? {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return manager.primaryClip?.getItemAt(0)?.text?.toString()
    }

    /**
     * Устанавливает фильтр ввода
     * @param digitsAfterDot количество символов после запятой
     */
    private fun setFilter(digitsAfterDot: Int?) {
        filters = arrayOf(CurrencyInputFilter(digitsAfterDot))
    }

    /**
     * Основной метод, в котором происходит рисование
     */
    override fun onDraw(canvas: Canvas?) {
        // Подсчет необходимы параметров расположения текста
        val zeros = getInsignificantZeros(text.toString())
        val textWidth = paint.measureText(text.toString())
        val zerosStart = paddingLeft.toFloat() + textWidth
        val currency = getCurrency()
        val zerosWidth = paint.measureText(zeros)
        val currencyStart = zerosStart + zerosWidth + currencySpacing

        // Рисуется значок валюты
        canvas?.drawText(currency, currencyStart, baseline.toFloat(), paint)

        // Рисуются вспомогательные нули
        val paintAlpha = paint.color.alpha
        paint.color = ColorUtils.setAlphaComponent(paint.color, paintAlpha / 2)
        canvas?.drawText(zeros, zerosStart, baseline.toFloat(), paint)

        // Рисуется основной текст
        super.onDraw(canvas)
    }

    /**
     * @return [String] символ установленной валюты
     */
    private fun getCurrency(): String {
        return when(currencyType) {
            CurrencyType.RUR -> {
                "₽"
            }
            CurrencyType.EUR -> {
                "€"
            }
            CurrencyType.USD -> {
                "$"
            }
        }
    }

    /**
     * Устанавливает значение в поле
     * @param currency числовое представление суммы
     */
    fun setValue(currency: BigDecimal) {
        val text = currency.toString()
        setText(text)
    }

    /**
     * @return [BigDecimal] числовое значение поля (может быть null)
     */
    fun getValue(): BigDecimal? {
        val value = text?.toString()?.replace(" ", "")?.toBigDecimalOrNull()
        return value
    }

    /**
     * Устанавливает колбэк на обработку изменения значения + состояния
     * @param action действие, которое необходимо совершить
     */
    fun setOnValueChanged(action: (BigDecimal?, state: State) -> Unit) {
        onValueChanged = object : OnValueChanged {
            override fun onValueChanged(newValue: BigDecimal?, state: State) {
                action.invoke(newValue, state)
            }
        }
    }

    /**
     * Интерефейс для колбэка изменения значения и состояния
     */
    interface OnValueChanged {
        fun onValueChanged(newValue: BigDecimal?, state: State)
    }

    /**
     * Статика
     */
    companion object {
        /**
         * Возможные состояния значения View
         */
        enum class State {
            OK,             // Валидное состояние (но текст еще изменяется)
            BAD_MIN_VALUE,  // Невалидное состояние (значение меньше минимума)
            BAD_MAX_VALUE,  // Невалидное состояние (значение больше максимума)
            APPLY           // Валидное состояние (текст больше не изменится, пока его опять не начнут редактировать)
        }

        /**
         * Возможные типы валюты
         */
        enum class CurrencyType {
            RUR, // Рубль
            EUR, // Евро
            USD  // Доллар
        }

        private const val DEFAULT_CURRENCY_SPACING = 2F // Начальный отступ значка валюты от текста
    }
}