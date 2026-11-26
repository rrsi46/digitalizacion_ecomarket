package bloque2.entrega4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Variante de extension: Problema clasico Lectores-Escritores aplicado al
 * Panel de Telemetria e Inventario de EcoFleet usando ReentrantReadWriteLock.
 */
public class PanelInventarioRW {
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private final AtomicInteger lectoresActivos = new AtomicInteger(0);
    private final AtomicInteger maxLectoresSimultaneos = new AtomicInteger(0);
    private final AtomicInteger lecturasDuranteEscritura = new AtomicInteger(0);

    private int stockDisponible = 20;
    private int version = 1;
    private String estadoMuelle = "OPERATIVO";

    public void leerInventario(String nombreLector) throws InterruptedException {
        rwLock.readLock().lock();
        try {
            int activos = lectoresActivos.incrementAndGet();
            maxLectoresSimultaneos.accumulateAndGet(activos, Math::max);
            System.out.printf("  [LECTOR - %s] Consulta v%d (Stock=%d, %s) | Lectores simultaneos: %d%n",
                    nombreLector, version, stockDisponible, estadoMuelle, activos);
            Thread.sleep(70);
        } finally {
            lectoresActivos.decrementAndGet();
            rwLock.readLock().unlock();
        }
    }

    public void actualizarInventario(String nombreEscritor, int deltaStock, String nuevoEstado)
            throws InterruptedException {
        rwLock.writeLock().lock();
        try {
            int lectoresDentro = lectoresActivos.get();
            if (lectoresDentro > 0) {
                lecturasDuranteEscritura.incrementAndGet();
            }
            this.stockDisponible += deltaStock;
            this.estadoMuelle = nuevoEstado;
            this.version++;
            System.out.printf("  [ESCRITOR - %s] Actualiza v%d -> Stock=%d (%s) | Lectores activos: %d%n",
                    nombreEscritor, version, stockDisponible, estadoMuelle, lectoresDentro);
            Thread.sleep(80);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public static boolean ejecutarSimulacion() throws InterruptedException {
        PanelInventarioRW panel = new PanelInventarioRW();
        List<Thread> hilos = new ArrayList<>();

        for (int i = 1; i <= 4; i++) {
            final String lector = "Monitor-" + i;
            hilos.add(new Thread(() -> {
                try {
                    panel.leerInventario(lector);
                    Thread.sleep(40);
                    panel.leerInventario(lector);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, lector));
        }

        hilos.add(new Thread(() -> {
            try {
                Thread.sleep(25);
                panel.actualizarInventario("Gestor-1", +15, "REABASTECIDO");
                Thread.sleep(30);
                panel.actualizarInventario("Gestor-2", -5, "ALTA_DEMANDA");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Gestor-Stock"));

        for (Thread t : hilos) {
            t.start();
        }
        for (Thread t : hilos) {
            t.join();
        }

        System.out.printf("  --> Maximo lectores simultaneos: %d | Lectores durante escritura: %d | Version final: v%d%n",
                panel.maxLectoresSimultaneos.get(), panel.lecturasDuranteEscritura.get(), panel.version);

        return panel.maxLectoresSimultaneos.get() >= 2 && panel.lecturasDuranteEscritura.get() == 0;
    }
}
