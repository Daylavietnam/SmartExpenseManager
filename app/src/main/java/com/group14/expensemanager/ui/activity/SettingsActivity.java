package com.group14.expensemanager.ui.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.group14.expensemanager.R;
import com.group14.expensemanager.data.TransactionRepository;
import com.group14.expensemanager.model.Transaction;
import com.group14.expensemanager.utils.CsvExporter;

import android.net.Uri;
import android.content.Intent;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executor;



public class SettingsActivity extends AppCompatActivity {
    private MaterialSwitch switchFingerprint;
    private MaterialSwitch switchPin;
    private MaterialButton btnExportCsv;
    private SharedPreferences sharedPreferences;
    private boolean isChangingSwitch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialButton btnCancel = findViewById(R.id.btnCancel);
        MaterialButton btnSave = findViewById(R.id.btnSave);
        btnExportCsv = findViewById(R.id.btnExportCsv);
        switchFingerprint = findViewById(R.id.switchFingerprint);
        switchPin = findViewById(R.id.switchPin);

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        boolean useFingerprint = sharedPreferences.getBoolean("UseFingerprint", false);
        boolean usePin = sharedPreferences.getBoolean("UsePin", false);

        if (useFingerprint && usePin) {
            usePin = false;
            sharedPreferences.edit().putBoolean("UsePin", false).apply();
        }

        switchFingerprint.setChecked(useFingerprint);
        switchPin.setChecked(usePin);

        switchPin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChangingSwitch) return;
            if (isChecked) {
                isChangingSwitch = true;
                switchFingerprint.setChecked(false);
                isChangingSwitch = false;
                sharedPreferences.edit().putBoolean("UseFingerprint", false).apply();
                showCreatePinDialog();
            } else {
                showDisablePinDialog();
            }
        });

        switchFingerprint.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChangingSwitch) return;
            if (isChecked) {
                isChangingSwitch = true;
                switchPin.setChecked(false);
                isChangingSwitch = false;
                sharedPreferences.edit().putBoolean("UsePin", false).apply();
                showBiometricPrompt();
            } else {
                sharedPreferences.edit().putBoolean("UseFingerprint", false).apply();
            }
        });

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> {
            sharedPreferences.edit()
                    .putBoolean("UsePin", switchPin.isChecked())
                    .putBoolean("UseFingerprint", switchFingerprint.isChecked())
                    .apply();
            Toast.makeText(this, "Đã lưu cài đặt!", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnExportCsv.setOnClickListener(v -> {
            TransactionRepository repository =
                    TransactionRepository.getInstance(
                            getApplication()
                    );
            repository.getTransactionsForExport(new TransactionRepository.ExportCallback() {
                        @Override
                        public void onResult(List<Transaction> transactions) {
                            File file = CsvExporter.exportToExcel(SettingsActivity.this, transactions);
                            runOnUiThread(() -> {
                                Toast.makeText(SettingsActivity.this, file == null ? "Không thể xuất báo cáo kho" : "Đã xuất báo cáo kho", Toast.LENGTH_SHORT).show();
                                if (file != null && file.exists()) {
                                    openFile(file);
                                }
                            });
                        }
                    }
            );
        });
    }

    private void openFile(File file) {
        Uri path = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(path, "application/vnd.ms-excel");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Không tìm thấy ứng dụng để mở tệp Excel", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCreatePinDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_create_pin, null);
        EditText edtPin = view.findViewById(R.id.edtPin);
        EditText edtConfirmPin = view.findViewById(R.id.edtConfirmPin);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Tạo mã PIN mới")
                .setView(view)
                .setPositiveButton("Xác nhận", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String pin = edtPin.getText().toString().trim();
                String confirm = edtConfirmPin.getText().toString().trim();
                if (pin.length() != 4) {
                    Toast.makeText(this, "PIN phải gồm 4 số", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!pin.equals(confirm)) {
                    Toast.makeText(this, "PIN nhập lại không đúng", Toast.LENGTH_SHORT).show();
                    return;
                }
                sharedPreferences.edit().putString("UserPin", pin).putBoolean("UsePin", true).apply();
                Toast.makeText(this, "Tạo PIN thành công", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
                isChangingSwitch = true;
                switchPin.setChecked(false);
                isChangingSwitch = false;
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void showBiometricPrompt() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK;

        if (biometricManager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Thiết bị không hỗ trợ hoặc chưa cài vân tay", Toast.LENGTH_SHORT).show();
            isChangingSwitch = true;
            switchFingerprint.setChecked(false);
            isChangingSwitch = false;
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                isChangingSwitch = true;
                switchFingerprint.setChecked(false);
                isChangingSwitch = false;
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                sharedPreferences.edit().putBoolean("UseFingerprint", true).apply();
                Toast.makeText(SettingsActivity.this, "Bật vân tay thành công", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(SettingsActivity.this, "Không nhận diện được vân tay", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Cài đặt vân tay")
                .setSubtitle("Xác nhận để bật đăng nhập bằng vân tay")
                .setNegativeButtonText("Hủy")
                .setAllowedAuthenticators(authenticators)
                .build();
        biometricPrompt.authenticate(promptInfo);
    }

    private void showDisablePinDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_disable_pin, null);
        EditText edtOldPin = view.findViewById(R.id.edtOldPin);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Xác nhận tắt mã PIN")
                .setView(view)
                .setPositiveButton("Xác nhận", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String input = edtOldPin.getText().toString().trim();
                String oldPin = sharedPreferences.getString("UserPin", "");
                if (input.equals(oldPin)) {
                    sharedPreferences.edit().remove("UserPin").putBoolean("UsePin", false).apply();
                    isChangingSwitch = true;
                    switchPin.setChecked(false);
                    isChangingSwitch = false;
                    Toast.makeText(this, "Đã tắt mã PIN", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Sai mã PIN cũ", Toast.LENGTH_SHORT).show();
                }
            });

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
                isChangingSwitch = true;
                switchPin.setChecked(true);
                isChangingSwitch = false;
                dialog.dismiss();
            });
        });
        dialog.show();
    }
}
