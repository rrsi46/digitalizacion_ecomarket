package bloque1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;

public class DiagnosticoServidor {
    public static void main(String[] args) {
        System.out.println("[EcoFleet] Iniciando diagnostico de red del servidor logistico...");
        
        try {
            // 1. Detectar el sistema operativo para adaptar el comando ping
            String sistemaOperativo = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (sistemaOperativo.contains("win")) {
                // Windows: ping -n 4 192.168.1.1 (4 paquetes)
                pb = new ProcessBuilder("ping", "-n", "4", "192.168.1.1");
            } else {
                // Linux / Mac: ping -c 4 192.168.1.1 (4 paquetes)
                pb = new ProcessBuilder("ping", "-c", "4", "192.168.1.1");
            }

            // Redirigir errores estándar al flujo estándar para capturarlo todo unificado
            pb.redirectErrorStream(true);

            // 2. Arrancar el proceso externo
            Process proceso = pb.start();

            // 3. Preparar flujos para leer la salida del proceso y escribir en "fichero.log"
            try (BufferedReader lectorProceso = new BufferedReader(new InputStreamReader(proceso.getInputStream()));
                 BufferedWriter escritorLog = new BufferedWriter(new FileWriter("fichero.log"))) {

                String linea;
                // 4. Leer línea a línea el resultado del comando ping
                while ((linea = lectorProceso.readLine()) != null) {
                    System.out.println(linea); // Muestra por consola
                    escritorLog.write(linea);  // Vuelca al fichero de log
                    escritorLog.newLine();
                }
            }

            // 5. Esperar a que el proceso termine y capturar su código de salida
            int codigoSalida = proceso.waitFor();
            System.out.println("\n[EcoFleet] Diagnostico finalizado. Codigo de salida (exitValue): " + codigoSalida);
            
            if (codigoSalida == 0) {
                System.out.println("[OK] Conexion con el router del centro logistico establecida con exito.");
            } else {
                System.out.println("[WARNING] El comando de ping devolvio un codigo de error: " + codigoSalida);
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Ocurrio un fallo al ejecutar el diagnostico: " + e.getMessage());
        }
    }
}