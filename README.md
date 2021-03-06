CurrencyEditText
================

`CurrencyEditText` is an extension for EditText with which it is convenient to get data about the amount of money.

Demo
================

<img src="https://user-images.githubusercontent.com/37814484/139474956-11e91162-9c9b-41bc-a9ec-ad3c1196e587.gif" width="300" height="500" />

How to use?
================

Declare View in XML:

```XML
<com.firmfreez.currencyedittext.views.CurrencyEditText
    android:id="@+id/currency"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginHorizontal="16dp"
    app:currencyType="USD"
    app:currencyDigitsAfterDot="3"
    app:currencyStartValue="123.101"
    app:layout_constraintBottom_toBottomOf="parent"
    app:layout_constraintLeft_toLeftOf="parent"
    app:layout_constraintRight_toRightOf="parent"
    app:layout_constraintTop_toTopOf="parent" />
```
        
After that, you can configure the View parameters separately in the code:

```Kotlin
binding.currency.minValue = BigDecimal.valueOf(100.125)
binding.currency.maxValue = BigDecimal.valueOf(200.019)
binding.currency.digitsAfterDot = 2
binding.currency.setValue(BigDecimal.valueOf(150.5))
```

You can set an event listener:

```Kotlin
binding.currency.setOnValueChanged { bigDecimal, state ->
    binding.valueText.text = "Current value: $bigDecimal"
    binding.stateText.text = "Current state: $state"
}
```


