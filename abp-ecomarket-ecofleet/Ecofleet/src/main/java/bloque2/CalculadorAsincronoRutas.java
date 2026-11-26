package bloque2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// 1. Clase que define el resultado a devolver por cada hilo
class ResultadoRuta {
    int idPedido;
    double costeEstimado;
    String rutaOptima;

    public ResultadoRuta(int idPedido, double costeEstimado, String rutaOptima) {
        this.idPedido = idPedido;
        this.costeEstimado = costeEstimado;
        this.rutaOptima = rutaOptima;
    }
}

// 2. La tarea Callable que procesará cada pedido de forma asíncrona
class CalculoRutaTask implements Callable<ResultadoRuta> {
    private int idPedido;

    public CalculoRutaTask(int idPedido) {
        this.idPedido = idPedido;
    }

    @Override
    public ResultadoRuta call() throws Exception {
        // Simulamos un cálculo complejo (conexión a API, algoritmos de ruta, etc.)
        Thread.sleep(10); // 10 milisegundos de bloqueo artificial por tarea
        double coste = 15.0 + (Math.random() * 50.0); 
        String ruta = "Ruta-Z" + (idPedido % 5);
        return new ResultadoRuta(idPedido, coste, ruta);
    }
}

// 3. Clase principal para ejecutar la comparativa
public class CalculadorAsincronoRutas {
    private static final int TOTAL_PEDIDOS = 500;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("[EcoFleet] Modulo de Calculo de Rutas y Costes");
        System.out.println("==================================================");
        
        // --- 1. PROCESADO SECUENCIAL (Clásico) ---
        System.out.println("\n[1] Iniciando calculo secuencial de " + TOTAL_PEDIDOS + " pedidos...");
        long inicioSecuencial = System.currentTimeMillis();
        
        for (int i = 1; i <= TOTAL_PEDIDOS; i++) {
            try {
                // Instanciamos y llamamos directamente .call() en el hilo principal
                CalculoRutaTask tarea = new CalculoRutaTask(i);
                tarea.call(); 
            } catch (Exception e) {
                System.err.println("Error en secuencial: " + e.getMessage());
            }
        }
        
        long finSecuencial = System.currentTimeMillis();
        long tiempoSecuencial = finSecuencial - inicioSecuencial;
        System.out.println("--> Calculo secuencial completado en: " + tiempoSecuencial + " ms");


        // --- 2. PROCESADO PARALELO (ExecutorService / Callable) ---
        System.out.println("\n[2] Iniciando calculo paralelo de " + TOTAL_PEDIDOS + " pedidos...");
        long inicioParalelo = System.currentTimeMillis();
        
        // Creamos un Pool de Hilos basado en los núcleos lógicos del sistema (ej: 16 en un i7)
        int nucleos = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(nucleos);
        
        // Lista para guardar las "promesas" de resultados futuros
        List<Future<ResultadoRuta>> futuros = new ArrayList<>();
        
        // Enviamos las 500 tareas al executor al mismo tiempo
        for (int i = 1; i <= TOTAL_PEDIDOS; i++) {
            CalculoRutaTask tarea = new CalculoRutaTask(i);
            futuros.add(executor.submit(tarea));
        }
        
        // Recolectamos los resultados de los hilos
        int completados = 0;
        for (Future<ResultadoRuta> futuro : futuros) {
            try {
                // .get() se bloquea hasta que el hilo en concreto termine y devuelva el ResultadoRuta
                ResultadoRuta resultado = futuro.get(); 
                completados++;
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error procesando pedido asincrono: " + e.getMessage());
            }
        }
        
        // Apagamos el executor para liberar recursos
        executor.shutdown();
        
        long finParalelo = System.currentTimeMillis();
        long tiempoParalelo = finParalelo - inicioParalelo;
        
        System.out.println("--> Calculo paralelo completado (" + completados + " pedidos) en: " + tiempoParalelo + " ms");


        // --- 3. RESUMEN DE GANANCIA DE TIEMPO ---
        System.out.println("\n==================================================");
        System.out.println("              COMPARATIVA DE TIEMPOS              ");
        System.out.println("==================================================");
        System.out.println("Tiempo Secuencial : " + tiempoSecuencial + " ms");
        System.out.println("Tiempo Paralelo   : " + tiempoParalelo + " ms");
        long ganancia = tiempoSecuencial - tiempoParalelo;
        double mejora = (double) tiempoSecuencial / tiempoParalelo;
        System.out.printf("Ganancia de tiempo: %d ms (%.2fx mas rapido)\n", ganancia, mejora);
        System.out.println("==================================================");
    }
}