package bloque2.entrega4;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Monitor compartido que representa el muelle de carga limitado de EcoFleet.
 * Sincroniza productores (brazos roboticos) y consumidores (camiones de reparto)
 * mediante bloques synchronized, wait() y notifyAll().
 */
public class ColaMuelle {
    private final Queue<Paquete> buffer = new LinkedList<>();
    private final int capacidadMaxima;
    private boolean produccionFinalizada = false;

    private int totalCargados = 0;
    private int totalRetirados = 0;
    private int esperasProductor = 0;
    private int esperasConsumidor = 0;

    public ColaMuelle(int capacidadMaxima) {
        if (capacidadMaxima <= 0) {
            throw new IllegalArgumentException("La capacidad del muelle debe ser mayor que 0.");
        }
        this.capacidadMaxima = capacidadMaxima;
    }

    /**
     * Deposita un paquete en el muelle. Si esta lleno, el brazo robotico espera.
     */
    public synchronized void cargarPaquete(Paquete paquete, String nombreBrazo) throws InterruptedException {
        while (buffer.size() == capacidadMaxima) {
            esperasProductor++;
            System.out.printf("  [ESPERA - %s] Muelle LLENO (%d/%d). Esperando hueco...%n",
                    nombreBrazo, buffer.size(), capacidadMaxima);
            wait();
        }

        buffer.add(paquete);
        totalCargados++;
        System.out.printf("  [PROD - %s] Carga %s -> Stock muelle: %d/%d%n",
                nombreBrazo, paquete, buffer.size(), capacidadMaxima);

        notifyAll();
    }

    /**
     * Retira un paquete del muelle. Si esta vacio y quedan productores activos, espera.
     * Devuelve null cuando la produccion ha finalizado y no quedan paquetes.
     */
    public synchronized Paquete retirarPaquete(String nombreCamion) throws InterruptedException {
        while (buffer.isEmpty() && !produccionFinalizada) {
            esperasConsumidor++;
            System.out.printf("  [ESPERA - %s] Muelle VACIO (0/%d). Esperando paquetes...%n",
                    nombreCamion, capacidadMaxima);
            wait();
        }

        if (buffer.isEmpty() && produccionFinalizada) {
            return null;
        }

        Paquete paquete = buffer.poll();
        totalRetirados++;
        System.out.printf("  [CONS - %s] Retira %s -> Stock muelle: %d/%d%n",
                nombreCamion, paquete, buffer.size(), capacidadMaxima);

        notifyAll();
        return paquete;
    }

    /**
     * Notifica a todos los consumidores en espera que no entraran mas paquetes.
     */
    public synchronized void finalizarProduccion() {
        this.produccionFinalizada = true;
        notifyAll();
    }

    public synchronized int getTotalCargados() {
        return totalCargados;
    }

    public synchronized int getTotalRetirados() {
        return totalRetirados;
    }

    public synchronized int getStockActual() {
        return buffer.size();
    }

    public synchronized int getEsperasProductor() {
        return esperasProductor;
    }

    public synchronized int getEsperasConsumidor() {
        return esperasConsumidor;
    }
}
