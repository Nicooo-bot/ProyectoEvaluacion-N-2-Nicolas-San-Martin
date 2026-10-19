package cl.ipvg.evaluacion2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class RegistroActivity extends AppCompatActivity implements SensorEventListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private EditText etTitulo, etDescripcion, etFechaHora, etLatitud, etLongitud;
    private Spinner spCategoria;
    private Button btnObtenerGPS, btnGuardar;

    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;

    // Sensor para limpiar formulario al agitar (RF06)
    private SensorManager sensorManager;
    private Sensor acelerometro;
    private long ultimaSacudida = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        etTitulo = findViewById(R.id.etTitulo);
        etDescripcion = findViewById(R.id.etDescripcion);
        etFechaHora = findViewById(R.id.etFechaHora);
        etLatitud = findViewById(R.id.etLatitud);
        etLongitud = findViewById(R.id.etLongitud);
        spCategoria = findViewById(R.id.spCategoria);
        btnObtenerGPS = findViewById(R.id.btnObtenerGPS);
        btnGuardar = findViewById(R.id.btnGuardar);

        // Categorías de inspección
        String[] categorias = {"Seleccione categoría...", "Infraestructura Vial", "Riesgo Eléctrico", "Fuga de Agua / Matriz", "Seguridad / Alumbrado", "Medio Ambiente"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categorias);
        spCategoria.setAdapter(adapter);

        // Autocompletar fecha y hora actual (RF02)
        String fechaActual = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        etFechaHora.setText(fechaActual);

        // Inicializar Firebase Firestore y Cliente de Ubicación GPS
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Inicializar Sensor (RF06)
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        btnObtenerGPS.setOnClickListener(v -> solicitarPermisoYObtenerUbicacion());
        btnGuardar.setOnClickListener(v -> validarYGuardarIncidencia());
    }

    // RF04: Solicitar permiso en tiempo de ejecución y obtener ubicación actual
    private void solicitarPermisoYObtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            obtenerCoordenadasGPS();
        }
    }

    private void obtenerCoordenadasGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Toast.makeText(this, "Obteniendo coordenadas GPS...", Toast.LENGTH_SHORT).show();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        etLatitud.setText(String.format(Locale.US, "%.6f", location.getLatitude()));
                        etLongitud.setText(String.format(Locale.US, "%.6f", location.getLongitude()));
                        Toast.makeText(this, "Ubicación GPS obtenida con éxito.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No se pudo obtener señal GPS activa. Verifique que la ubicación esté encendida o ingrésela manualmente.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Toast.makeText(this, "Error al consultar GPS: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // RF04: Contemplar permiso concedido o denegado
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerCoordenadasGPS();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado. Puede ingresar las coordenadas manualmente.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // RF02 y RF03: Validar campos obligatorios y guardar en Firestore controlando errores de red
    private void validarYGuardarIncidencia() {
        String titulo = etTitulo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        int posCategoria = spCategoria.getSelectedItemPosition();
        String categoria = spCategoria.getSelectedItem().toString();
        String fechaHora = etFechaHora.getText().toString().trim();
        String latStr = etLatitud.getText().toString().trim();
        String lonStr = etLongitud.getText().toString().trim();

        if (TextUtils.isEmpty(titulo)) {
            etTitulo.setError("El título es obligatorio");
            etTitulo.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(descripcion)) {
            etDescripcion.setError("La descripción es obligatoria");
            etDescripcion.requestFocus();
            return;
        }
        if (posCategoria == 0) {
            Toast.makeText(this, "Seleccione una categoría válida", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(fechaHora)) {
            etFechaHora.setError("La fecha y hora son obligatorias");
            etFechaHora.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(latStr)) {
            etLatitud.setError("Latitud obligatoria");
            etLatitud.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(lonStr)) {
            etLongitud.setError("Longitud obligatoria");
            etLongitud.requestFocus();
            return;
        }

        double latitud, longitud;
        try {
            latitud = Double.parseDouble(latStr.replace(",", "."));
            longitud = Double.parseDouble(lonStr.replace(",", "."));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Formato de coordenadas inválido.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
            Toast.makeText(this, "Coordenadas fuera de rango geográfico válido.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar estado de red antes de enviar para mostrar advertencia sin cerrar la app (RF03)
        if (!hayConexionRed()) {
            Toast.makeText(this, "Error de red: Sin conexión a Internet. Verifique su conectividad e intente nuevamente.", Toast.LENGTH_LONG).show();
            return;
        }

        btnGuardar.setEnabled(false);
        String id = UUID.randomUUID().toString();
        incidencia nuevaIncidencia = new incidencia(id, titulo, descripcion, categoria, fechaHora, latitud, longitud);

        db.collection("incidencias")
                .document(id)
                .set(nuevaIncidencia)
                .addOnSuccessListener(aVoid -> {
                    btnGuardar.setEnabled(true);
                    Toast.makeText(RegistroActivity.this, "Incidencia registrada correctamente en Firestore.", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnGuardar.setEnabled(true);
                    Toast.makeText(RegistroActivity.this, "Error al guardar en Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private boolean hayConexionRed() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    private void limpiarFormulario() {
        etTitulo.setText("");
        etDescripcion.setText("");
        spCategoria.setSelection(0);
        etLatitud.setText("");
        etLongitud.setText("");
        String fechaActual = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        etFechaHora.setText(fechaActual);
        Toast.makeText(this, "Sacudida detectada (Sensor): Formulario limpiado.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (acelerometro != null) {
            sensorManager.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (acelerometro != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double magnitud = Math.sqrt(x * x + y * y + z * z);

            if (magnitud > 18.0) {
                long ahora = System.currentTimeMillis();
                if (ahora - ultimaSacudida > 2000) {
                    ultimaSacudida = ahora;
                    limpiarFormulario();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
