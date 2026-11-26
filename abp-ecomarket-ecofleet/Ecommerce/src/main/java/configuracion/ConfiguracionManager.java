package configuracion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
  Gestor encargado de inicializar el entorno de directorios del proyecto 
  y de administrar la lectura y escritura del archivo de configuración properties.
 */
public class ConfiguracionManager {
    // Definición de constantes para las rutas del directorio de datos y del fichero de configuración
    private static final String DIR_PATH = "./data/e_commerce/";
    private static final String CONFIG_FILE = DIR_PATH + "config.properties";

    /**
      Inicializa el entorno de trabajo de la aplicación.
      Comprueba si el directorio de datos existe y lo crea si es necesario.
      También verifica si existe el archivo 'config.properties' y, de no existir, 
      lo crea automáticamente con los valores por defecto del sistema.
     */
    public static void inicializarEntorno() {
        try {
            // Comprobación y creación del directorio base de datos
            File directorio = new File(DIR_PATH);
            if (!directorio.exists()) {
                if (directorio.mkdirs()) {
                    System.out.println("[INFO] Directorio creado: " + DIR_PATH);
                }
            }

            // Comprobación y creación por defecto del archivo de configuración
            File archivoConfig = new File(CONFIG_FILE);
            if (!archivoConfig.exists()) {
                Properties props = new Properties();
                props.setProperty("app.nombre", "E_commerce Store");
                props.setProperty("app.iva", "21.0");
                props.setProperty("path.catalogo", DIR_PATH + "productos.dat");

                // Escritura de las propiedades iniciales en el disco
                try (FileOutputStream fos = new FileOutputStream(archivoConfig)) {
                    props.store(fos, "Configuracion inicial de E-Commerce");
                    System.out.println("[INFO] Archivo config.properties creado por defecto.");
                }
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Al inicializar la configuracion: " + e.getMessage());
        }
    }

    //Obtiene el valor asociado a una clave específica dentro del archivo 'config.properties'.

    public static String getProperty(String clave) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            props.load(fis);
            return props.getProperty(clave);
        } catch (IOException e) {
            System.err.println("[ERROR] No se pudo leer config.properties: " + e.getMessage());
            return null;
        }
    }
}