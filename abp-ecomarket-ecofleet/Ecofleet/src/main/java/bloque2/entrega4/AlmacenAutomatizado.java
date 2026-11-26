package bloque2.entrega4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Entrega 4 - EcoFleet Logistica & Monitoring System:
 * El Almacen Automatizado (Sincronizacion de hilos y problemas de concurrencia).
 */
public class AlmacenAutomatizado {

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("[EcoFleet] Entrega 4: El Almacen Automatizado (Sync Hilos)");
        System.out.println("============================================================");

        try {
            // --- FASE 1: EJERCICIO BASE (Productor-Consumidor con synchronized / wait / notifyAll) ---
            boolean okFase1 = ejecutarProductorConsumidorBase();

            // --- FASE 2: ESTRUCTURAS CONCURRENTES AVANZADAS ---
            System.out.println("\n[FASE 2] Concurrencia Avanzada (ArrayBlockingQueue, Semaphore, ConcurrentHashMap, AtomicInteger)");
            System.out.println("------------------------------------------------------------");
            boolean okFase2 = GestorDarsenasConcurrente.ejecutarSimulacion();

            // --- FASE 3: LECTORES-ESCRITORES (ReentrantReadWriteLock) ---
            System.out.println("\n[FASE 3] Problema Clasico Lectores-Escritores (ReentrantReadWriteLock)");
            System.out.println("------------------------------------------------------------");
            boolean okFase3 = PanelInventarioRW.ejecutarSimulacion();

            // --- FASE 4: DEADLOCK DELIBERADO Y PREVENCION ---
            System.out.println("\n[FASE 4A] Provocacion y Deteccion de Deadlock (ThreadMXBean)");
            System.out.println("------------------------------------------------------------");
            boolean okDeadlock = SimuladorDeadlock.provocarYDetectarDeadlock();

            System.out.println("\n[FASE 4B] Prevencion de Deadlock (Ordenacion de Recursos)");
            System.out.println("------------------------------------------------------------");
            boolean okPrevencion = SimuladorDeadlock.ejecutarPrevencionLockOrdering();

            // --- RESUMEN FINAL DE VERIFICACION ---
            System.out.println("\n============================================================");
            System.out.println("             RESUMEN DE VERIFICACION - ENTREGA 4            ");
            System.out.println("============================================================");
            System.out.println("1. Productor-Consumidor (synchronized/wait/notifyAll): " + (okFase1 ? "OK" : "ERROR"));
            System.out.println("2. Estructuras Concurrentes (Queue/Semaphore/Map/Atom): " + (okFase2 ? "OK" : "ERROR"));
            System.out.println("3. Lectores-Escritores (ReentrantReadWriteLock)      : " + (okFase3 ? "OK" : "ERROR"));
            System.out.println("4. Deadlock provocado y prevenido (Lock Ordering)    : " + (okDeadlock && okPrevencion ? "OK" : "ERROR"));
            System.out.println("============================================================");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[ERROR] Simulacion interrumpida: " + e.getMessage());
        }
    }

    private static boolean ejecutarProductorConsumidorBase() throws InterruptedException {
        final int capacidadMuelle = 3;
        final int numBrazos = 2;
        final int paquetesPorBrazo = 4;
        final int numCamiones = 3;

        System.out.println("\n[FASE 1] Productor-Consumidor en Muelle de Carga (Capacidad = " + capacidadMuelle + ")");
        System.out.println("------------------------------------------------------------");

        ColaMuelle muelle = new ColaMuelle(capacidadMuelle);
        AtomicInteger generadorIds = new AtomicInteger(0);
        AtomicInteger totalRetirados = new AtomicInteger(0);

        List<Thread> productores = new ArrayList<>();
        for (int i = 1; i <= numBrazos; i++) {
            Thread p = new Thread(
                    new BrazoRobotico("Brazo-" + i, muelle, generadorIds, paquetesPorBrazo, 30),
                    "Brazo-" + i
            );
            productores.add(p);
        }

        List<Thread> consumidores = new ArrayList<>();
        for (int i = 1; i <= numCamiones; i++) {
            // Retardo mayor que el productor para provocar que el muelle alcance capacidad maxima (wait en productor)
            Thread c = new Thread(
                    new CamionReparto("Camion-" + i, muelle, 115, totalRetirados),
                    "Camion-" + i
            );
            consumidores.add(c);
        }

        for (Thread c : consumidores) {
            c.start();
        }
        for (Thread p : productores) {
            p.start();
        }

        for (Thread p : productores) {
            p.join();
        }
        muelle.finalizarProduccion();

        for (Thread c : consumidores) {
            c.join();
        }

        int esperados = numBrazos * paquetesPorBrazo;
        System.out.printf("  --> Cargados: %d | Retirados: %d | Stock residual: %d | Esperas (Prod=%d, Cons=%d)%n",
                muelle.getTotalCargados(), muelle.getTotalRetirados(), muelle.getStockActual(),
                muelle.getEsperasProductor(), muelle.getEsperasConsumidor());

        return muelle.getTotalCargados() == esperados
                && muelle.getTotalRetirados() == esperados
                && muelle.getStockActual() == 0;
    }
}
