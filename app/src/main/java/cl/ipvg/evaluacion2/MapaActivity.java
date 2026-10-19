package cl.ipvg.evaluacion2;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MapaActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        db = FirebaseFirestore.getInstance();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Si viene desde un clic en ListadoActivity, mostrar esa incidencia en particular
        if (getIntent().hasExtra("latitud") && getIntent().hasExtra("longitud")) {
            String titulo = getIntent().getStringExtra("titulo");
            String categoria = getIntent().getStringExtra("categoria");
            double lat = getIntent().getDoubleExtra("latitud", -36.6063);
            double lon = getIntent().getDoubleExtra("longitud", -72.1023);

            LatLng ubicacion = new LatLng(lat, lon);
            mMap.addMarker(new MarkerOptions()
                    .position(ubicacion)
                    .title(titulo)
                    .snippet("Categoría: " + categoria)).showInfoWindow();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, 15f));
        } else {
            // Cargar todos los marcadores guardados en Firestore (RF05)
            cargarMarcadoresDesdeFirestore();
        }
    }

    private void cargarMarcadoresDesdeFirestore() {
        db.collection("incidencias")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    LatLng ultimaUbicacion = null;
                    int contador = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            incidencia inc = doc.toObject(incidencia.class);
                            LatLng punto = new LatLng(inc.getLatitud(), inc.getLongitud());
                            mMap.addMarker(new MarkerOptions()
                                    .position(punto)
                                    .title(inc.getTitulo())
                                    .snippet(inc.getCategoria() + " - " + inc.getFechaHora()));
                            ultimaUbicacion = punto;
                            contador++;
                        } catch (Exception e) {
                            // Ignorar errores de parseo
                        }
                    }

                    if (ultimaUbicacion != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ultimaUbicacion, 13f));
                        Toast.makeText(this, "Se cargaron " + contador + " incidencias en el mapa.", Toast.LENGTH_SHORT).show();
                    } else {
                        // Ubicación por defecto (Chillán, Chile) si aún no hay registros
                        LatLng chillan = new LatLng(-36.6063, -72.1023);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(chillan, 12f));
                        Toast.makeText(this, "No hay incidencias registradas aún para mostrar marcadores.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar marcadores desde Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}