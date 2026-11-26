package bloque2.entrega4;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Variante de extension: Uso de estructuras concurrentes avanzadas de Java
 * (ArrayBlockingQueue, Semaphore, ConcurrentHashMap y AtomicInteger).
 */
public class GestorDarsenasConcurrente {
    private static final String[] ZONAS = {"Zona-Norte", "Zona-Centro", "Zona-Sur"};

    public static boolean ejecutarSimulacion() throws InterruptedException {
        final int capacidadCinta = 4;
        final int darsenasFisicas = 2;
        final int numAlimentadores = 2;
        final int paquetesPorAlimentador = 4;
        final int numCamiones = 4;

        ArrayBlockingQueue<Paquete> cintaAutomatica = new ArrayBlockingQueue<>(capacidadCinta);
        Semaphore darsenasCarga = new Semaphore(darsenasFisicas, true);
        ConcurrentHashMap<String, AtomicInteger> resumenZonas = new ConcurrentHashMap<>();
        AtomicInteger secuenciaId = new AtomicInteger(100);
        AtomicInteger totalDespachados = new AtomicInteger(0);
        AtomicInteger alimentadoresActivos = new AtomicInteger(numAlimentadores);

        List<Thread> hilos = new ArrayList<>();

        for (int i = 1; i <= numAlimentadores; i++) {
            final int idAlim = i;
            Thread t = new Thread(() -> {
                try {
                    for (int k = 0; k < paquetesPorAlimentador; k++) {
                        Thread.sleep(35);
                        int id = secuenciaId.incrementAndGet();
                        String zona = ZONAS[id % ZONAS.length];
                        Paquete p = new Paquete(id, zona, 8.0 + (id % 10));
                        cintaAutomatica.put(p);
                        System.out.printf("  [CINTA - Alim-%d] Encola %s (Cola: %d/%d)%n",
                                idAlim, p, cintaAutomatica.size(), capacidadCinta);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    alimentadoresActivos.decrementAndGet();
                }
            }, "Alimentador-" + i);
            hilos.add(t);
        }

        for (int i = 1; i <= numCamiones; i++) {
            final String camion = "Camion-ADV-" + i;
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        if (alimentadoresActivos.get() == 0 && cintaAutomatica.isEmpty()) {
                            break;
                        }
                        darsenasCarga.acquire();
                        try {
                            Paquete p = cintaAutomatica.poll(80, TimeUnit.MILLISECONDS);
                            if (p != null) {
                                int activos = darsenasFisicas - darsenasCarga.availablePermits();
                                System.out.printf("  [DARSENA %d/%d - %s] Despacha %s%n",
                                        activos, darsenasFisicas, camion, p);
                                resumenZonas
                                        .computeIfAbsent(p.getZonaDestino(), z -> new AtomicInteger(0))
                                        .incrementAndGet();
                                totalDespachados.incrementAndGet();
                                Thread.sleep(55);
                            }
                        } finally {
                            darsenasCarga.release();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, camion);
            hilos.add(t);
        }

        for (Thread t : hilos) {
            t.start();
        }
        for (Thread t : hilos) {
            t.join();
        }

        int esperados = numAlimentadores * paquetesPorAlimentador;
        System.out.println("  --> Resumen ConcurrentHashMap por zona:");
        for (Map.Entry<String, AtomicInteger> entry : resumenZonas.entrySet()) {
            System.out.printf("      * %-12s: %d paquetes%n", entry.getKey(), entry.getValue().get());
        }
        System.out.printf("  --> Total despachados (AtomicInteger): %d / %d%n",
                totalDespachados.get(), esperados);

        return totalDespachados.get() == esperados;
    }
}
