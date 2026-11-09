package com.arr.svgtovector;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.arr.svgtovector.databinding.ActivityMainBinding;
import com.arr.svgtovector.utils.SvgConverter;
import com.arr.svgtovector.utils.VectorPreview;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import android.content.res.Configuration;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private final ActivityResultLauncher<String[]> picker =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri != null) {
                            binding.etFileName.setText(suggestName(uri));
                            convert(uri);
                        }
                    });
    private String currentXml = "";

    private final ActivityResultLauncher<String> saver =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument("text/xml"),
                    uri -> {
                        if (uri != null) save(uri);
                    });

    private static final String PREFS = "settings";
    private static final String KEY_FOLDER = "folder_uri";
    private static final int RW_FLAGS =
            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

    private Uri folderUri;
    private boolean pendingSave = false;

    private final ActivityResultLauncher<Uri> folderPicker =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocumentTree(),
                    uri -> {
                        if (uri == null) {
                            pendingSave = false;
                            return;
                        }
                        releaseFolderPermission(); // suelta la carpeta anterior
                        getContentResolver().takePersistableUriPermission(uri, RW_FLAGS);
                        folderUri = uri;
                        getSharedPreferences(PREFS, MODE_PRIVATE)
                                .edit()
                                .putString(KEY_FOLDER, uri.toString())
                                .apply();
                        updateFolderUi();
                        if (pendingSave) {
                            pendingSave = false;
                            saveToFolder();
                        }
                    });
    private Uri lastUri;

    // 1 = etiqueta, 2 = nombre de atributo, 3 = "=", 4 = valor, 5 = cierre
    private static final Pattern XML_TOKEN =
            Pattern.compile("(</?[\\w:.\\-]+)|([\\w:.\\-]+)(=)|(\"[^\"]*\")|(/?>)");

    private CharSequence highlight(String xml) {
        xml = xml.replaceAll("(?s)<!--.*?-->\\s*", "");
        boolean dark =
                (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_YES;

        int cTag = dark ? 0xFFE8BF6A : 0xFF0033B3;
        int cAttr = dark ? 0xFF9876AA : 0xFF871094;
        int cValue = dark ? 0xFF6A8759 : 0xFF067D17;

        SpannableString s = new SpannableString(xml);
        Matcher m = XML_TOKEN.matcher(xml);
        while (m.find()) {
            int group, color;
            if (m.group(1) != null) {
                group = 1;
                color = cTag;
            } else if (m.group(2) != null) {
                group = 2;
                color = cAttr;
            } else if (m.group(4) != null) {
                group = 4;
                color = cValue;
            } else {
                group = 5;
                color = cTag;
            }
            s.setSpan(
                    new ForegroundColorSpan(color),
                    m.start(group),
                    m.end(group),
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return s;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLoad.setOnClickListener(
                v -> picker.launch(new String[] {"image/svg+xml", "text/xml"}));

        binding.btnCopy.setOnClickListener(
                v -> {
                    ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("vector", currentXml));
                    Toast.makeText(this, "Copiado", Toast.LENGTH_SHORT).show();
                });

        binding.btnExport.setOnClickListener(
                v -> {
                    if (currentXml.isEmpty()) {
                        Toast.makeText(this, "Primero carga un SVG", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String name = sanitizeFileName(binding.etFileName.getText().toString());
                    binding.etFileName.setText(name);
                    saver.launch(name + ".xml");
                });
        binding.etTint.setText("?attr/colorControlNormal");
        binding.etTint.addTextChangedListener(
                new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

                    @Override
                    public void onTextChanged(CharSequence s, int a, int b, int c) {}

                    @Override
                    public void afterTextChanged(android.text.Editable s) {
                        if (lastUri != null) convert(lastUri);
                    }
                });
        binding.etFileName.setText("ic_vector");

        String saved = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_FOLDER, null);
        if (saved != null) folderUri = Uri.parse(saved);
        updateFolderUi();

        binding.btnPickFolder.setOnClickListener(v -> folderPicker.launch(null));
        binding.btnClearFolder.setOnClickListener(
                v -> {
                    clearFolder();
                    Toast.makeText(this, "Ruta eliminada", Toast.LENGTH_SHORT).show();
                });
        binding.btnSave.setOnClickListener(v -> saveToFolder());
    }

    private void save(Uri uri) {
        try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
            out.write(currentXml.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "Archivo guardado", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void convert(Uri uri) {
        lastUri = uri;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            String xml = SvgConverter.toVectorDrawable(in, binding.etTint.getText().toString());
            currentXml = xml;
            binding.tvResult.setText(highlight(xml));
            binding.ivPreview.setImageBitmap(VectorPreview.render(xml, 512));
            binding.textSvgPreview.setVisibility(View.GONE);
            binding.ivPreview.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            currentXml = "";
            binding.textSvgPreview.setVisibility(View.VISIBLE);
            binding.ivPreview.setVisibility(View.GONE);
            binding.ivPreview.setImageDrawable(null);
            binding.tvResult.setText("Error: " + e.getMessage());
        }
    }

    private String sanitizeFileName(String raw) {
        String name = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (name.endsWith(".xml")) name = name.substring(0, name.length() - 4);
        name = name.replaceAll("[^a-z0-9_]", "_");
        if (name.isEmpty()) name = "ic_vector";
        if (Character.isDigit(name.charAt(0))) name = "ic_" + name;
        return name;
    }

    private String getDisplayName(Uri uri) {
        try (Cursor c =
                getContentResolver()
                        .query(
                                uri,
                                new String[] {OpenableColumns.DISPLAY_NAME},
                                null,
                                null,
                                null)) {
            if (c != null && c.moveToFirst()) {
                String name = c.getString(0);
                if (name != null) return name;
            }
        } catch (Exception ignored) {
        }
        String last = uri.getLastPathSegment();
        return last != null ? last : "vector";
    }

    private String suggestName(Uri uri) {
        String base = getDisplayName(uri).replaceAll("(?i)\\.svg$", "");
        String name = sanitizeFileName(base).replaceAll("_+", "_").replaceAll("^_|_$", "");
        if (name.isEmpty()) name = "vector";
        if (!name.startsWith("ic_")) name = "ic_" + name;
        return name;
    }

    private void updateFolderUi() {
        if (folderUri == null) {
            binding.tvFolder.setText("Sin carpeta seleccionada");
            binding.btnClearFolder.setEnabled(false);
            return;
        }
        DocumentFile dir = DocumentFile.fromTreeUri(this, folderUri);
        String name = (dir != null && dir.getName() != null) ? dir.getName() : folderUri.toString();
        binding.tvFolder.setText("Carpeta: " + name);
        binding.btnClearFolder.setEnabled(true);
    }

    private void releaseFolderPermission() {
        if (folderUri == null) return;
        try {
            getContentResolver().releasePersistableUriPermission(folderUri, RW_FLAGS);
        } catch (Exception ignored) {
        }
    }

    private void clearFolder() {
        releaseFolderPermission();
        folderUri = null;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(KEY_FOLDER).apply();
        updateFolderUi();
    }

    private void saveToFolder() {
        if (currentXml.isEmpty()) {
            Toast.makeText(this, "Primero carga un SVG", Toast.LENGTH_SHORT).show();
            return;
        }
        if (folderUri == null) { // primera vez: pide la carpeta y guarda al elegirla
            pendingSave = true;
            folderPicker.launch(null);
            return;
        }

        DocumentFile dir = DocumentFile.fromTreeUri(this, folderUri);
        if (dir == null || !dir.exists() || !dir.canWrite()) {
            clearFolder();
            Toast.makeText(this, "La carpeta ya no está disponible. Elige otra.", Toast.LENGTH_LONG)
                    .show();
            pendingSave = true;
            folderPicker.launch(null);
            return;
        }

        String name = sanitizeFileName(binding.etFileName.getText().toString());
        binding.etFileName.setText(name);

        DocumentFile existing = dir.findFile(name + ".xml");
        if (existing != null) {
            new AlertDialog.Builder(this)
                    .setTitle("El archivo ya existe")
                    .setMessage(name + ".xml ya existe en la carpeta. ¿Reemplazarlo?")
                    .setPositiveButton("Reemplazar", (d, w) -> save(existing.getUri()))
                    .setNegativeButton("Cancelar", null)
                    .show();
        } else {
            DocumentFile file = dir.createFile("text/xml", name + ".xml");
            if (file == null) {
                Toast.makeText(this, "No se pudo crear el archivo", Toast.LENGTH_LONG).show();
                return;
            }
            save(file.getUri());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
