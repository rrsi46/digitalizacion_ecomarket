package bloque2.entrega4;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hilo Consumidor: Camion de reparto que retira paquetes del muelle compartido.
 */
public class CamionReparto implements Runnable {
    private final String nombre;
    private final ColaMuelle muelle;
    private final long retardoMs;
    private final AtomicInteger totalGlobalRetirados;
    private int retiradosPropios = 0;

    public CamionReparto(String nombre, ColaMuelle muelle, long retardoMs,
                         AtomicInteger totalGlobalRetirados) {
        this.nombre = nombre;
        this.muelle = muelle;
        this.retardoMs = retardoMs;
        this.totalGlobalRetirados = totalGlobalRetirados;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Paquete paquete = muelle.retirarPaquete(nombre);
                if (paquete == null) {
                    break;
                }
                retiradosPropios++;
                totalGlobalRetirados.incrementAndGet();
                Thread.sleep(retardoMs);
            }
            System.out.printf("  [FIN CONS - %s] Sale a reparto con %d paquetes.%n",
                    nombre, retiradosPropios);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.printf("  [INTERRUPCION - %s] Camion detenido de forma controlada.%n", nombre);
        }
    }
}
