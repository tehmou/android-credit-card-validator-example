package com.tehmou.book.androidcreditcardvalidatorexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.rxbinding2.widget.RxTextView;
import com.tehmou.book.androidcreditcardvalidatorexample.utils.CardType;
import com.tehmou.book.androidcreditcardvalidatorexample.utils.ValidationUtils;

import java.util.Arrays;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private EditText creditCardNumberView;
    private EditText creditCardCvcView;
    private TextView creditCardType;
    private TextView errorText;
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resolveViews();

        submitButton.setOnClickListener((view) -> {
            Log.d(TAG, "Submit");
            findViewById(R.id.container).requestFocus();
        });

        final Observable<String> creditCardNumber =
                RxTextView.textChanges(creditCardNumberView)
                        .map(CharSequence::toString);

        final Observable<String> creditCardCvc =
                RxTextView.textChanges(creditCardCvcView)
                        .map(CharSequence::toString);

        final Observable<CardType> cardType =
                creditCardNumber
                        .map(CardType::fromNumber);

        final Observable<Boolean> isKnownCardType =
                cardType
                        .map(cardTypeValue -> cardTypeValue != CardType.UNKNOWN);

        final Observable<Boolean> isValidCheckSum =
                creditCardNumber
                        .map(ValidationUtils::checkCardChecksum);

        final Observable<Boolean> isValidNumber =
                Observable.combineLatest(
                        isKnownCardType,
                        isValidCheckSum,
                        (isValidType, isChecksumCorrect) -> isValidType && isChecksumCorrect);

        final Observable<Boolean> isValidCvc =
                Observable.combineLatest(
                        cardType,
                        creditCardCvc,
                        ValidationUtils::isValidCvc);

        cardType
                .map(Enum::toString)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(creditCardType::setText);

        Observable.combineLatest(
                isValidNumber,
                isValidCvc,
                (isValidNumberValue, isValidCvcValue) -> isValidNumberValue && isValidCvcValue)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(submitButton::setEnabled);

        Observable.combineLatest(
                Arrays.asList(
                        isKnownCardType.map(value -> value ? "" : "Unknown card type"),
                        isValidCheckSum.map(value -> value ? "" : "Invalid checksum"),
                        isValidCvc.map(value -> value ? "" : "Invalid CVC code")),
                (errorStrings) -> {
                    StringBuilder builder = new StringBuilder();
                    for (Object errorString : errorStrings) {
                        if (!"".equals(errorString)) {
                            builder.append(errorString);
                            builder.append("\n");
                        }
                    }
                    return builder.toString();
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(errorText::setText);
    }

    private void resolveViews() {
        creditCardNumberView = (EditText) findViewById(R.id.credit_card_number);
        creditCardCvcView = (EditText) findViewById(R.id.credit_card_cvc);
        creditCardType = (TextView) findViewById(R.id.credit_card_type);
        errorText = (TextView) findViewById(R.id.error_text);
        submitButton = (Button) findViewById(R.id.submit_button);
    }
}
