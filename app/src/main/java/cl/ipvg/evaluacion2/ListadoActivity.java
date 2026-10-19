package cl.ipvg.evaluacion2;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ListadoActivity extends AppCompatActivity {

    private TextView tvEstadoCarga;
    private ListView lvIncidencias;
    private Button btnRecargar;

    private FirebaseFirestore db;
    private List<incidencia> listaObjetos;
    private List<String> listaTextos;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listado);

        tvEstadoCarga = findViewById(R.id.tvEstadoCarga);
        lvIncidencias = findViewById(R.id.lvIncidencias);
        btnRecargar = findViewById(R.id.btnRecargar);

        db = FirebaseFirestore.getInstance();
        listaObjetos = new ArrayList<>();
        listaTextos = new ArrayList<>();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaTextos);
        lvIncidencias.setAdapter(adapter);

        // Al tocar una incidencia, abrirla directamente en el mapa (RF01, RF05)
        lvIncidencias.setOnItemClickListener((parent, view, position, id) -> {
            incidencia seleccionada = listaObjetos.get(position);
            Intent intent = new Intent(ListadoActivity.this, MapaActivity.class);
            intent.putExtra("titulo", seleccionada.getTitulo());
            intent.putExtra("categoria", seleccionada.getCategoria());
            intent.putExtra("latitud", seleccionada.getLatitud());
            intent.putExtra("longitud", seleccionada.getLongitud());
            startActivity(intent);
        });

        btnRecargar.setOnClickListener(v -> cargarIncidencias());

        cargarIncidencias();
    }

    private void cargarIncidencias() {
        if (!hayConexionRed()) {
            tvEstadoCarga.setText("⚠️ Sin conexión a Internet. Mostrando caché local o verifique su red.");
            Toast.makeText(this, "Error de red: No hay conexión a Internet activa.", Toast.LENGTH_LONG).show();
        } else {
            tvEstadoCarga.setText("Consultando Firebase Cloud Firestore...");
        }

        db.collection("incidencias")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaObjetos.clear();
                    listaTextos.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            incidencia inc = doc.toObject(incidencia.class);
                            listaObjetos.add(inc);
                            String item = "📌 " + inc.getTitulo() + " [" + inc.getCategoria() + "]\n"
                                    + "📝 " + inc.getDescripcion() + "\n"
                                    + "🕒 " + inc.getFechaHora() + " | 🌐 (" + inc.getLatitud() + ", " + inc.getLongitud() + ")";
                            listaTextos.add(item);
                        } catch (Exception e) {
                            // Ignorar documentos malformados sin cerrar la app
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (listaObjetos.isEmpty()) {
                        tvEstadoCarga.setText("No hay incidencias registradas todavía.");
                    } else {
                        tvEstadoCarga.setText("Total registradas: " + listaObjetos.size() + " (Toque una para ver en el mapa)");
                    }
                })
                .addOnFailureListener(e -> {
                    tvEstadoCarga.setText("Error al recuperar datos: " + e.getMessage());
                    Toast.makeText(ListadoActivity.this, "No se pudo conectar con Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
}