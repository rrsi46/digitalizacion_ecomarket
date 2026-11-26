package bloque2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;

public class AnalizadorLogs {

    public static void main(String[] args) {
        String ficheroEntrada = "syslog.txt";
        String ficheroSalida = "PANEL-SYSLOG.out";
        
        int countCritical = 0;
        int countError = 0;
        int countWarning = 0;
        int countInfo = 0;
        int totalLineas = 0;

        System.out.println("[EcoFleet] Analizando flujo de syslog mediante subproceso (pipes)...");

        try {
            // 1. Crear el subproceso en lugar de leer el archivo directamente
            String sistemaOperativo = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            
            if (sistemaOperativo.contains("win")) {
                // En Windows usamos 'cmd /c type' para imprimir el contenido del archivo
                pb = new ProcessBuilder("cmd", "/c", "type", ficheroEntrada);
            } else {
                // En Linux/Mac usamos 'cat'
                pb = new ProcessBuilder("cat", ficheroEntrada);
            }

            // 2. Arrancar el proceso
            Process proceso = pb.start();

            // 3. Conectar el PIPE: Leemos la salida estándar del subproceso
            // Esto aplica la jerarquía explicada por el profesor: proceso.getInputStream() -> InputStreamReader -> BufferedReader
            try (BufferedReader br = new BufferedReader(new InputStreamReader(proceso.getInputStream()))) {
                String linea;
                
                while ((linea = br.readLine()) != null) {
                    totalLineas++;
                    
                    // Filtrado y clasificación (Opción A: Fácil)
                    if (linea.contains("CRITICAL")) {
                        countCritical++;
                    } else if (linea.contains("ERROR")) {
                        countError++;
                    } else if (linea.contains("WARNING")) {
                        countWarning++;
                    } else if (linea.contains("INFO")) {
                        countInfo++;
                    }
                }
            }
            
            // Esperamos a que el subproceso termine
            proceso.waitFor();

        } catch (Exception e) {
            System.err.println("[ERROR] No se pudo ejecutar el subproceso para leer el syslog: " + e.getMessage());
            return;
        }

        // Escritura del informe final en PANEL-SYSLOG.out (Esto se mantiene igual)
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ficheroSalida))) {
            bw.write("CRITICAL: " + countCritical);
            bw.newLine();
            bw.write("ERROR: " + countError);
            bw.newLine();
            bw.write("WARNING: " + countWarning);
            bw.newLine();
            bw.write("INFO: " + countInfo);
            bw.newLine();
            bw.write("TOTAL LINEAS: " + totalLineas);
            bw.newLine();
            
            // Forzar el vaciado del búfer a disco
            bw.flush();
            System.out.println("[EcoFleet] Analisis completado con exito. Resultados guardados en " + ficheroSalida);
            
        } catch (IOException e) {
            System.err.println("[ERROR] No se pudo escribir el fichero de salida: " + e.getMessage());
        }
    }
}