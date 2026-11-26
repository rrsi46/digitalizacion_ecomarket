package bloque2.entrega4;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Variante de extension: Provocacion controlada de un Deadlock (interbloqueo),
 * deteccion mediante ThreadMXBean y prevencion por Ordenacion Total de Recursos.
 */
public class SimuladorDeadlock {

    static class RecursoMuelle {
        final int id;
        final String nombre;
        final ReentrantLock lock = new ReentrantLock();

        RecursoMuelle(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
    }

    /**
     * Fase A: Provoca deliberadamente un deadlock cruzado entre dos brazos roboticos
     * y lo detecta programaticamente antes de abortar los hilos de forma limpia.
     */
    public static boolean provocarYDetectarDeadlock() throws InterruptedException {
        RecursoMuelle cintaNorte = new RecursoMuelle(1, "Cinta-Norte");
        RecursoMuelle gruaSur = new RecursoMuelle(2, "Grua-Sur");
        CountDownLatch ambosConPrimerLock = new CountDownLatch(2);

        Thread robotAlfa = new Thread(() -> {
            cintaNorte.lock.lock();
            try {
                System.out.println("  [Robot-Alfa] Bloquea " + cintaNorte.nombre);
                ambosConPrimerLock.countDown();
                ambosConPrimerLock.await();
                System.out.println("  [Robot-Alfa] Esperando " + gruaSur.nombre + "...");
                gruaSur.lock.lockInterruptibly();
                try {
                    System.out.println("  [Robot-Alfa] Transferencia completada.");
                } finally {
                    gruaSur.lock.unlock();
                }
            } catch (InterruptedException e) {
                System.out.println("  [Robot-Alfa] Liberado tras deteccion de interbloqueo.");
            } finally {
                if (cintaNorte.lock.isHeldByCurrentThread()) {
                    cintaNorte.lock.unlock();
                }
            }
        }, "Robot-Alfa");

        Thread robotBeta = new Thread(() -> {
            gruaSur.lock.lock();
            try {
                System.out.println("  [Robot-Beta] Bloquea " + gruaSur.nombre);
                ambosConPrimerLock.countDown();
                ambosConPrimerLock.await();
                System.out.println("  [Robot-Beta] Esperando " + cintaNorte.nombre + "...");
                cintaNorte.lock.lockInterruptibly();
                try {
                    System.out.println("  [Robot-Beta] Transferencia completada.");
                } finally {
                    cintaNorte.lock.unlock();
                }
            } catch (InterruptedException e) {
                System.out.println("  [Robot-Beta] Liberado tras deteccion de interbloqueo.");
            } finally {
                if (gruaSur.lock.isHeldByCurrentThread()) {
                    gruaSur.lock.unlock();
                }
            }
        }, "Robot-Beta");

        robotAlfa.start();
        robotBeta.start();

        // Esperar brevemente a que ambos entren en espera circular
        Thread.sleep(300);

        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] deadlockedIds = bean.findDeadlockedThreads();
        boolean deadlockDetectado = (deadlockedIds != null && deadlockedIds.length >= 2);

        if (deadlockDetectado) {
            System.out.println("  [DETECTOR ThreadMXBean] DEADLOCK DETECTADO entre " + deadlockedIds.length + " hilos:");
            ThreadInfo[] infos = bean.getThreadInfo(deadlockedIds);
            for (ThreadInfo info : infos) {
                System.out.printf("      -> Hilo '%s' (estado=%s) bloqueado por '%s'%n",
                        info.getThreadName(), info.getThreadState(), info.getLockOwnerName());
            }
        }

        // Interrupcion controlada para que la JVM no quede colgada
        robotAlfa.interrupt();
        robotBeta.interrupt();
        robotAlfa.join();
        robotBeta.join();

        return deadlockDetectado;
    }

    /**
     * Fase B: Previene el deadlock imponiendo un orden global estricto por ID de recurso.
     */
    public static boolean ejecutarPrevencionLockOrdering() throws InterruptedException {
        RecursoMuelle cintaNorte = new RecursoMuelle(1, "Cinta-Norte");
        RecursoMuelle gruaSur = new RecursoMuelle(2, "Grua-Sur");
        AtomicInteger operacionesCompletadas = new AtomicInteger(0);

        Thread robotAlfa = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                adquirirEnOrden("Robot-Alfa", cintaNorte, gruaSur, i);
                operacionesCompletadas.incrementAndGet();
            }
        }, "Robot-Alfa-Seguro");

        Thread robotBeta = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                // Aunque solicite los recursos en orden inverso, el protocolo los ordena por ID
                adquirirEnOrden("Robot-Beta", gruaSur, cintaNorte, i);
                operacionesCompletadas.incrementAndGet();
            }
        }, "Robot-Beta-Seguro");

        robotAlfa.start();
        robotBeta.start();
        robotAlfa.join();
        robotBeta.join();

        long[] deadlockedIds = ManagementFactory.getThreadMXBean().findDeadlockedThreads();
        boolean sinDeadlock = (deadlockedIds == null);
        System.out.printf("  --> Operaciones completadas sin interbloqueo: %d/6 | Deadlocks: %s%n",
                operacionesCompletadas.get(), sinDeadlock ? "0 (NINGUNO)" : "DETECTADO");

        return sinDeadlock && operacionesCompletadas.get() == 6;
    }

    private static void adquirirEnOrden(String robot, RecursoMuelle r1, RecursoMuelle r2, int ciclo) {
        RecursoMuelle primero = (r1.id < r2.id) ? r1 : r2;
        RecursoMuelle segundo = (r1.id < r2.id) ? r2 : r1;

        primero.lock.lock();
        try {
            segundo.lock.lock();
            try {
                System.out.printf("  [%s] Ciclo %d: adquiere %s (id=%d) -> %s (id=%d) [OK]%n",
                        robot, ciclo, primero.nombre, primero.id, segundo.nombre, segundo.id);
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                segundo.lock.unlock();
            }
        } finally {
            primero.lock.unlock();
        }
    }
}
