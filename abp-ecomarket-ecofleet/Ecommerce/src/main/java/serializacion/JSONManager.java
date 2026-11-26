package serializacion;

import modelo.Producto;
import ficheros.ProductoRAFDAO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class JSONManager {

    private ProductoRAFDAO dao;

    public JSONManager(ProductoRAFDAO dao) {
        this.dao = dao;
    }

    // ==========================================
    // 1. IMPORTACIÓN MASIVA JSON A DAT (JACKSON)
    // ==========================================
    public void importarJSONMasivo(File archivoJson) {
        if (!archivoJson.exists()) {
            System.out.println("[AVISO] El fichero JSON no existe.");
            return;
        }

        // Vaciado opcional del binario al importar masivamente (igual que en CSV)
        File fBinario = new File(dao.getRutaFichero());
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(fBinario, "rw")) {
            raf.setLength(0);
        } catch (Exception e) {
            // Si no existe se creará al insertar
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            // Jackson mapea el JSON directamente a un Array de objetos Producto
            Producto[] productosLeidos = mapper.readValue(archivoJson, Producto[].class);
            int insertados = 0;

            for (Producto p : productosLeidos) {
                // Inserta cada producto en tu archivo binario (productos.dat) a través del DAO
                if (dao.insertar(p)) {
                    insertados++;
                }
            }
            System.out.println("[INFO] Importacion JSON completada (Jackson). Productos insertados en DAT: " + insertados);
        } catch (Exception e) {
            System.err.println("[ERROR] Al importar JSON con Jackson: " + e.getMessage());
        }
    }

    // ==========================================
    // 2. VISUALIZAR ARCHIVO 
    // ==========================================
    public void visualizarJSON(File archivoJSON) {
        if (!archivoJSON.exists()) {
            System.out.println("[AVISO] El fichero JSON no existe todavia. Importa uno primero.");
            return;
        }
        System.out.println("\n--- CONTENIDO DEL ARCHIVO JSON ORIGINAL (" + archivoJSON.getName() + ") ---");
        try (BufferedReader br = new BufferedReader(new FileReader(archivoJSON))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                System.out.println(linea);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Al leer el fichero JSON: " + e.getMessage());
        }
    }

    // ==========================================
    // 3. EXPORTACIÓN A JSON (JACKSON)
    // ==========================================
    public void exportarJSON(File archivoDestino, int maxId) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        List<Producto> listaProductos = new ArrayList<>();

        for (int i = 1; i <= maxId; i++) {
            Producto p = dao.buscarPorId(i);
            if (p != null) {
                listaProductos.add(p);
            }
        }

        try {
            mapper.writeValue(archivoDestino, listaProductos);
            System.out.println("[INFO] Exportacion a JSON completada (Jackson). Productos exportados: " + listaProductos.size());
        } catch (Exception e) {
            System.err.println("[ERROR] Al exportar a JSON con Jackson: " + e.getMessage());
        }
    }
}
