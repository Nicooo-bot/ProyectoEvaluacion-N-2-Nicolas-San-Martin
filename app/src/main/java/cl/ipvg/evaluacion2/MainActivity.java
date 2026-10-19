package cl.ipvg.evaluacion2;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private Button btnIrRegistro, btnIrListado, btnIrMapa;
    private TextView tvEstadoSensor, tvValoresSensor;

    private SensorManager sensorManager;
    private Sensor acelerometro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnIrRegistro = findViewById(R.id.btnIrRegistro);
        btnIrListado = findViewById(R.id.btnIrListado);
        btnIrMapa = findViewById(R.id.btnIrMapa);
        tvEstadoSensor = findViewById(R.id.tvEstadoSensor);
        tvValoresSensor = findViewById(R.id.tvValoresSensor);

        // Navegación principal (RF01)
        btnIrRegistro.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RegistroActivity.class)));
        btnIrListado.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ListadoActivity.class)));
        btnIrMapa.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, MapaActivity.class)));

        // Configuración y control de disponibilidad del Sensor (RF06)
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        if (acelerometro == null) {
            tvEstadoSensor.setText("Estado: Este dispositivo NO cuenta con sensor acelerómetro.");
            tvValoresSensor.setText("Sensor no disponible");
        } else {
            tvEstadoSensor.setText("Acelerómetro activo (Estable)");
        }
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

            tvValoresSensor.setText(String.format(Locale.US, "X: %.2f | Y: %.2f | Z: %.2f m/s²", x, y, z));

            // Magnitud de aceleración para detectar movimiento brusco en terreno
            double magnitud = Math.sqrt(x * x + y * y + z * z);
            if (magnitud > 14.0) {
                tvEstadoSensor.setText("⚠️ Estado: ¡Movimiento brusco / Vibración detectada!");
            } else {
                tvEstadoSensor.setText("✅ Estado: Dispositivo estable en terreno");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No requiere acción adicional
    }
}