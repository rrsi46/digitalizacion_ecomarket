package ficheros;

import modelo.Producto;
import java.io.File;
import java.io.RandomAccessFile;

//DAO para la gestión de productos mediante ficheros de acceso aleatorio (RandomAccessFile).
//Cada registro tiene un tamaño fijo de 77 bytes.
 
public class ProductoRAFDAO {
    public static final int TAMANO_REGISTRO = 77; // 4 + 60 + 8 + 4 + 1
    private String rutaFichero;

    public ProductoRAFDAO(String rutaFichero) {
        this.rutaFichero = rutaFichero;
        verificarFichero();
    }

    public String getRutaFichero() {
        return this.rutaFichero;
    }

    // Verifica la existencia del fichero binario y lo crea vacío si no existe.
    
    private void verificarFichero() {
        File f = new File(rutaFichero);
        if (!f.exists()) {
            try {
                if (f.getParentFile() != null) {
                    f.getParentFile().mkdirs();
                }
                f.createNewFile();
            } catch (Exception e) {
                System.err.println("[ERROR] No se pudo crear el fichero binario: " + e.getMessage());
            }
        }
    }

    //Ajusta la cadena de texto del nombre a exactamente 30 caracteres para mantener el tamaño fijo.
     
    private String ajustarCadena(String texto) {
        if (texto.length() > 30) {
            return texto.substring(0, 30);
        }
        return String.format("%-30s", texto);
    }

    // Inserta o actualiza un producto en el fichero binario utilizando la fórmula de desplazamiento.
     
    public boolean insertar(Producto p) {
        // Si el stock es 0, el producto no está activo
        if (p.getStock() == 0) {
            p.setActivo(false);
        }

        try (RandomAccessFile raf = new RandomAccessFile(rutaFichero, "rw")) {
            long posicion;
            if (p.getId() <= 0) {
                // Añade el registro al final del fichero si el ID no es válido
                posicion = raf.length();
                int nuevoId = (int) (raf.length() / TAMANO_REGISTRO) + 1;
                p.setId(nuevoId);
            } else {
                // Posición exacta calculada con la fórmula: (ID - 1) * 77
                posicion = (long) (p.getId() - 1) * TAMANO_REGISTRO;
            }

            raf.seek(posicion);
            raf.writeInt(p.getId());
            raf.writeChars(ajustarCadena(p.getNombre())); // 30 caracteres * 2 bytes = 60 bytes
            raf.writeDouble(p.getPrecio());
            raf.writeInt(p.getStock());
            raf.writeBoolean(p.isActivo());
            return true;
        } catch (Exception e) {
            System.err.println("[ERROR] Al insertar producto: " + e.getMessage());
            return false;
        }
    }

    //  Busca y retorna un producto del fichero binario dado su ID utilizando acceso directo.
 
    public Producto buscarPorId(int id) {
        long posicion = (long) (id - 1) * TAMANO_REGISTRO;
        try (RandomAccessFile raf = new RandomAccessFile(rutaFichero, "r")) {
            if (posicion + TAMANO_REGISTRO > raf.length()) {
                return null;
            }
            raf.seek(posicion);
            int leidoId = raf.readInt();
            
            if (leidoId == 0) return null; // Registro marcado como eliminado físicamente

            char[] charArray = new char[30];
            for (int i = 0; i < 30; i++) {
                charArray[i] = raf.readChar();
            }
            String nombre = new String(charArray).trim();
            
            double precio = raf.readDouble();
            int stock = raf.readInt();
            boolean activo = raf.readBoolean();

            return new Producto(leidoId, nombre, precio, stock, activo);

        } catch (Exception e) {
            System.err.println("[ERROR] Al buscar producto por ID: " + e.getMessage());
            return null;
        }
    }

  
// Modifica el stock de un producto y actualiza automáticamente el campo activo
    public boolean modificarStock(int id, int nuevoStock) {
        long posicion = (long) (id - 1) * TAMANO_REGISTRO;
        try (RandomAccessFile raf = new RandomAccessFile(rutaFichero, "rw")) {
            if (posicion + TAMANO_REGISTRO > raf.length()) return false;
            
            // 1. Posicionarse justo en el stock: ID(4) + Nombre(60) + Precio(8) = 72 bytes
            raf.seek(posicion + 72);
            raf.writeInt(nuevoStock);
            
            // 2. Posicionarse en el campo activo (72 + 4 bytes del stock = 76 bytes)
            raf.seek(posicion + 76);
            // Si el stock es mayor que 0, activo pasa a true; si es 0, pasa a false
            raf.writeBoolean(nuevoStock > 0);
            
            return true;
        } catch (Exception e) {
            System.err.println("[ERROR] Al modificar stock: " + e.getMessage());
            return false;
        }
    }

    // Elimina físicamente un registro del fichero binario sobrescribiéndolo con valores vacíos (ID 0).
    
    public boolean eliminarProducto(int id) {
        long posicion = (long) (id - 1) * TAMANO_REGISTRO;
        try (RandomAccessFile raf = new RandomAccessFile(rutaFichero, "rw")) {
            if (posicion + TAMANO_REGISTRO > raf.length()) return false;
            
            raf.seek(posicion);
            raf.writeInt(0); // ID 0 indica registro eliminado
            raf.writeChars(String.format("%-30s", ""));
            raf.writeDouble(0.0);
            raf.writeInt(0);
            raf.writeBoolean(false);
            return true;
        } catch (Exception e) {
            System.err.println("[ERROR] Al eliminar el producto: " + e.getMessage());
            return false;
        }
    }

    // Recorre y lista todos los productos válidos almacenados en el fichero binario.
   
    public void listarProductos() {
        File f = new File(rutaFichero);
        if (!f.exists()) {
            System.out.println("[AVISO] Todavia no existe el fichero de productos. Importa un CSV primero.");
            return;
        }
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            long totalRegistros = raf.length() / TAMANO_REGISTRO;
            if (totalRegistros == 0) {
                System.out.println("[AVISO] El fichero binario esta vacio.");
                return;
            }

            System.out.println("\n--- LISTADO COMPLETO DE MODIFICACIONES (productos.dat) ---");
            int contadorMostrados = 0;
            for (int i = 1; i <= totalRegistros; i++) {
                Producto p = buscarPorId(i);
                if (p != null) {
                    System.out.println(p);
                    contadorMostrados++;
                }
            }
            if (contadorMostrados == 0) {
                System.out.println("[AVISO] No hay productos para mostrar.");
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Al listar los productos: " + e.getMessage());
        }
    }
}
