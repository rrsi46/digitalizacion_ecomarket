package bloque2.entrega4;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hilo Productor: Brazo robotico automatizado que deposita paquetes en el muelle.
 */
public class BrazoRobotico implements Runnable {
    private static final String[] ZONAS = {"Zona-Norte", "Zona-Centro", "Zona-Sur"};

    private final String nombre;
    private final ColaMuelle muelle;
    private final AtomicInteger generadorIds;
    private final int paquetesAProducir;
    private final long retardoMs;

    public BrazoRobotico(String nombre, ColaMuelle muelle, AtomicInteger generadorIds,
                         int paquetesAProducir, long retardoMs) {
        this.nombre = nombre;
        this.muelle = muelle;
        this.generadorIds = generadorIds;
        this.paquetesAProducir = paquetesAProducir;
        this.retardoMs = retardoMs;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < paquetesAProducir; i++) {
                Thread.sleep(retardoMs);
                int id = generadorIds.incrementAndGet();
                String zona = ZONAS[(id - 1) % ZONAS.length];
                double peso = 4.0 + ((id * 3.5) % 18.0);
                Paquete paquete = new Paquete(id, zona, peso);
                muelle.cargarPaquete(paquete, nombre);
            }
            System.out.printf("  [FIN PROD - %s] %d paquetes procesados.%n", nombre, paquetesAProducir);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.printf("  [INTERRUPCION - %s] Brazo detenido de forma controlada.%n", nombre);
        }
    }
}
