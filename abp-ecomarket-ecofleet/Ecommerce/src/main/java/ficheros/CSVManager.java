package ficheros;

import modelo.Producto;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class CSVManager {

    private ProductoRAFDAO dao;

    public CSVManager(ProductoRAFDAO dao) {
        this.dao = dao;
    }

    public int importarCSV(File archivoCSV) {
        int contador = 0;

        // VACIADO Abrir el fichero binario y poner su tamaño a 0 bytes
        File fBinario = new File(dao.getRutaFichero());
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(fBinario, "rw")) {
            raf.setLength(0); // Borra todo el contenido interno del .dat al instante
        } catch (Exception e) {
            // Si el archivo no existe todavía, no pasa nada, se creará al insertar
        }

        try (BufferedReader br = new BufferedReader(new FileReader(archivoCSV))) {
            String linea;
            boolean primeraLinea = true; // Para saltar la cabecera
            while ((linea = br.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false;
                    continue;
                }
                // Ajusta el split según el separador de tu CSV (coma ',' o barra '|')
            String[] partes = linea.split("\\|");
                if (partes.length == 5) {
                    int id = Integer.parseInt(partes[0].trim());
                    String nombre = partes[1].trim();
                    double precio = Double.parseDouble(partes[2].trim());
                    int stock = Integer.parseInt(partes[3].trim());
                    boolean activo = Boolean.parseBoolean(partes[4].trim());

                    Producto p = new Producto(id, nombre, precio, stock, activo);
                    if (dao.insertar(p)) {
                        contador++;
                    }
                }
            }
            System.out.println("[INFO] Importacion completada. Registros insertados: " + contador);
        } catch (Exception e) {
            System.err.println("[ERROR] Al importar CSV: " + e.getMessage());
        }
        return contador;
    }

    public void visualizarCSV(File archivoCSV) {
        if (!archivoCSV.exists()) {
            System.out.println("[AVISO] El fichero CSV no existe todavia. Importa uno primero.");
            return;
        }
        System.out.println("\n--- CONTENIDO DEL ARCHIVO CSV ORIGINAL (" + archivoCSV.getName() + ") ---");
        try (BufferedReader br = new BufferedReader(new FileReader(archivoCSV))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                System.out.println(linea);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Al leer el fichero CSV: " + e.getMessage());
        }
    }
    
    public void exportarCSV(File archivoDestino, int maxId) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivoDestino))) {
            // Cabecera separada por comas
            bw.write("ID,NOMBRE,PRECIO,STOCK,ACTIVO");
            bw.newLine();
            int exportados = 0;

            for (int i = 1; i <= maxId; i++) {
                Producto p = dao.buscarPorId(i);
                if (p != null && p.isActivo()) {
                    // Datos separados por comas
                    bw.write(String.format("%d,%s,%.2f,%d,%b",
                            p.getId(), p.getNombre(), p.getPrecio(), p.getStock(), p.isActivo()));
                    bw.newLine();
                    exportados++;
                }
            }
            System.out.println("[INFO] Exportacion completada a CSV. Productos activos exportados: " + exportados);
        } catch (Exception e) {
            System.err.println("[ERROR] Al exportar CSV: " + e.getMessage());
        }
    }
}
