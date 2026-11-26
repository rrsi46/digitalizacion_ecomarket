package bloque2.entrega4;

import java.net.InetSocketAddress;
import java.net.Socket;

public class EcofleetCloudMain {

    public static void main(String[] args) {
        String dbHost = System.getenv().getOrDefault("DB_HOST", "ecomarket-db");
        int dbPort = Integer.parseInt(System.getenv().getOrDefault("DB_PORT", "3306"));
        boolean modoServicio = Boolean.parseBoolean(System.getenv().getOrDefault("SERVICE_MODE", "true"));

        System.out.println("[EcoFleet-Cloud] Verificando conexion en red privada con " + dbHost + ":" + dbPort + "...");
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(dbHost, dbPort), 3000);
            System.out.println("[EcoFleet-Cloud] Conexion TCP establecida con MariaDB (" + dbHost + ":" + dbPort + ") [OK]");
        } catch (Exception e) {
            System.err.println("[EcoFleet-Cloud] No se pudo contactar con MariaDB: " + e.getMessage());
        }

        AlmacenAutomatizado.main(args);

        if (modoServicio) {
            System.out.println("\n[EcoFleet-Cloud] Microservicio activo en red privada. Esperando nuevas tareas...");
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}