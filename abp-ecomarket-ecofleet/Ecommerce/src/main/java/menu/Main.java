package menu;

import modelo.Producto;
import serializacion.JSONManager;
import serializacion.XMLManager;
import ficheros.ProductoRAFDAO;
import ficheros.CSVManager;
import configuracion.ConfiguracionManager;
import hibernate.GestorHibernate;
import hibernate.HibernateUtil;
import jdbc.GestorJDBC; 

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        // Inicialización exacta de tu entorno
        ConfiguracionManager.inicializarEntorno();
        String rutaCatalogo = ConfiguracionManager.getProperty("path.catalogo");

        File ficheroDat = new File(rutaCatalogo);
        if (ficheroDat.exists()) {
            ficheroDat.delete();
        }

        ProductoRAFDAO dao = new ProductoRAFDAO(rutaCatalogo);
        CSVManager csvManager = new CSVManager(dao);
        XMLManager xmlManager = new XMLManager(dao);
        JSONManager jsonManager = new JSONManager(dao);
        
        // Inicialización del nuevo Gestor JDBC y Gestor ORM Hibernate
        GestorJDBC gestorJDBC = new GestorJDBC();
        GestorHibernate gestorHibernate = new GestorHibernate();

        Scanner scanner = new Scanner(System.in);
        int opcionPrincipal;

        do {
            System.out.println("\n=== MENU PRINCIPAL E-COMMERCE ===");
            System.out.println("1. Ficheros, DAT, XML, JSON");
            System.out.println("2. Bases de Datos Relacionales - JDBC");
            System.out.println("3. Bases de Datos Relacionales - ORM (Hibernate / JPA)");
            System.out.println("0. Salir de la aplicacion");
            System.out.print("Elige una opcion: ");

            opcionPrincipal = leerEntero(scanner);

            switch (opcionPrincipal) {
                case 1:
                    mostrarSubmenuFicheros(scanner, dao, csvManager, xmlManager, jsonManager, rutaCatalogo);
                    break;
                case 2:
                    mostrarSubmenuJDBC(scanner, gestorJDBC);
                    break;
                case 3:
                    mostrarSubmenuHibernate(scanner, gestorHibernate);
                    break;
                case 0:
                    System.out.println("Saliendo de la aplicacion...");
                    HibernateUtil.shutdown();
                    break;
                default:
                    System.out.println("Opcion no valida.");
            }
        } while (opcionPrincipal != 0);

        scanner.close();
    }

    private static int leerEntero(Scanner scanner) {
        while (true) {
            String linea = scanner.nextLine().trim();
            try {
                return Integer.parseInt(linea);
            } catch (NumberFormatException e) {
                System.out.print("Introduce un numero valido: ");
            }
        }
    }

    /**
     * SUBMENÚ 1: 
     */
    private static void mostrarSubmenuFicheros(Scanner scanner, ProductoRAFDAO dao, CSVManager csvManager, 
                                               XMLManager xmlManager, JSONManager jsonManager, String rutaCatalogo) {
        int opcion;
        File archivoCsvReferencia = null;
        File archivoJsonReferencia = null;

        do {
            System.out.println("\n=== SUBMENU: FICHEROS ===");
            System.out.println("1. Importar catalogo desde CSV inicial");
            System.out.println("2. Visualizar archivo CSV original");
            System.out.println("3. Modificar stock de un producto (permite stock en 0)");
            System.out.println("4. Listar modificaciones actuales (productos.dat)");
            System.out.println("5. Insertar un nuevo producto (por ID o al final)");
            System.out.println("6. Eliminar un producto (del archivo DAT)");
            System.out.println("7. Buscar producto por ID (RandomAccessFile)");
            System.out.println("8. Exportar productos activos a CSV");
            System.out.println("9. Exportar catalogo a XML (DOM)");
            System.out.println("10. Importar catalogo masivo desde XML (SAX)");
            System.out.println("11. Exportar catalogo a JSON (Jackson)");
            System.out.println("12. Importar catalogo masivo desde JSON (Jackson)");
            System.out.println("13. Visualizar archivo JSON original");
            System.out.println("0. Volver al menu principal");
            System.out.print("Elige una opcion: ");

            opcion = scanner.nextInt();
            scanner.nextLine(); // Limpiar buffer de entrada

            switch (opcion) {
                case 1:
                    System.out.print("Introduce la ruta del fichero CSV (ej: productos.csv): ");
                    String rutaCsv = scanner.nextLine();
                    archivoCsvReferencia = new File(rutaCsv);
                    csvManager.importarCSV(archivoCsvReferencia);
                    break;
                case 2:
                    if (archivoCsvReferencia == null || !archivoCsvReferencia.exists()) {
                        System.out.println("[AVISO] Todavia no se ha importado ningun archivo CSV.");
                    } else {
                        csvManager.visualizarCSV(archivoCsvReferencia);
                    }
                    break;
                case 3:
                    System.out.print("Introduce el ID del producto: ");
                    int idStock = scanner.nextInt();
                    System.out.print("Introduce el nuevo stock (puede ser 0): ");
                    int nuevoStock = scanner.nextInt();
                    if (dao.modificarStock(idStock, nuevoStock)) {
                        System.out.println("[EXITO] Stock actualizado correctamente.");
                    } else {
                        System.out.println("[ERROR] No se pudo actualizar el stock.");
                    }
                    break;
                case 4:
                    dao.listarProductos();
                    break;
                case 5:
                    System.out.print("Introduce el ID (0 o negativo para anadir al final automaticamente): ");
                    int idNuevo = scanner.nextInt();
                    scanner.nextLine();
                    System.out.print("Introduce el nombre del producto (max. 30 caracteres): ");
                    String nombreNuevo = scanner.nextLine();
                    System.out.print("Introduce el precio: ");
                    double precioNuevo = scanner.nextDouble();
                    System.out.print("Introduce el stock: ");
                    int stockNuevo = scanner.nextInt();

                    boolean activoNuevo = stockNuevo > 0;

                    Producto nuevoProd = new Producto(idNuevo, nombreNuevo, precioNuevo, stockNuevo, activoNuevo);
                    if (dao.insertar(nuevoProd)) {
                        System.out.println("[EXITO] Producto insertado correctamente con ID: " + nuevoProd.getId());
                    } else {
                        System.out.println("[ERROR] No se pudo insertar el producto.");
                    }
                    break;
                case 6:
                    System.out.print("Introduce el ID del producto que desea eliminar: ");
                    int idEliminar = scanner.nextInt();
                    if (dao.eliminarProducto(idEliminar)) {
                        System.out.println("[EXITO] Producto eliminado correctamente del archivo DAT.");
                    } else {
                        System.out.println("[ERROR] No se pudo eliminar el producto.");
                    }
                    break;
                case 7:
                    System.out.print("Introduce el ID del producto a buscar: ");
                    int idBuscar = scanner.nextInt();
                    Producto p = dao.buscarPorId(idBuscar);
                    if (p != null) {
                        System.out.println("Resultado: " + p);
                    } else {
                        System.out.println("[AVISO] Producto no encontrado o eliminado.");
                    }
                    break;
                case 8:
                    System.out.print("Introduce la ruta del fichero CSV de salida (ej: exportados.csv): ");
                    String rutaSalida = scanner.nextLine();
                    try (RandomAccessFile raf = new RandomAccessFile(rutaCatalogo, "r")) {
                        int maxId = (int) (raf.length() / ProductoRAFDAO.TAMANO_REGISTRO);
                        csvManager.exportarCSV(new File(rutaSalida), maxId);
                    } catch (Exception e) {
                        System.out.println("[ERROR] No se pudo calcular el tamano para exportar.");
                    }
                    break;
                case 9:
                    System.out.print("Introduce la ruta del fichero XML de salida (ej: exportados.xml): ");
                    String rutaXmlSalida = scanner.nextLine();
                    try (RandomAccessFile raf = new RandomAccessFile(rutaCatalogo, "r")) {
                        int maxId = (int) (raf.length() / ProductoRAFDAO.TAMANO_REGISTRO);
                        xmlManager.exportarXML(new File(rutaXmlSalida), maxId);
                    } catch (Exception e) {
                        System.out.println("[ERROR] No se pudo calcular el tamano para exportar a XML.");
                    }
                    break;
                case 10:
                    System.out.print("Introduce la ruta del fichero masivo XML (ej: exportados.xml): ");
                    String rutaMasivoXml = scanner.nextLine();
                    xmlManager.importarXMLMasivoSAX(new File(rutaMasivoXml));
                    break;
                case 11:
                    System.out.print("Introduce la ruta del fichero JSON de salida (ej: productos.json): ");
                    String rutaJsonSalida = scanner.nextLine();
                    try (RandomAccessFile raf = new RandomAccessFile(rutaCatalogo, "r")) {
                        int maxId = (int) (raf.length() / ProductoRAFDAO.TAMANO_REGISTRO);
                        jsonManager.exportarJSON(new File(rutaJsonSalida), maxId);
                    } catch (Exception e) {
                        System.out.println("[ERROR] No se pudo calcular el tamaño para exportar a JSON.");
                    }
                    break;
                case 12:
                    System.out.print("Introduce la ruta del fichero masivo JSON (ej: productos.json): ");
                    String rutaMasivoJson = scanner.nextLine();
                    archivoJsonReferencia = new File(rutaMasivoJson); 
                    jsonManager.importarJSONMasivo(archivoJsonReferencia);
                    break;
                case 13:
                    if (archivoJsonReferencia == null || !archivoJsonReferencia.exists()) {
                        System.out.println("[AVISO] Todavia no se ha importado ningun archivo JSON.");
                    } else {
                        jsonManager.visualizarJSON(archivoJsonReferencia);
                    }
                    break;
                case 0:
                    System.out.println("Volviendo al menú principal...");
                    break;
                default:
                    System.out.println("Opcion no valida.");
            }
        } while (opcion != 0);
    }

    /**
     * SUBMENÚ 2: Operaciones JDBC para la Entrega 3
     */
    private static void mostrarSubmenuJDBC(Scanner scanner, GestorJDBC gestor) {
        int opcion;
        do {
            System.out.println("\n=== SUBMENU: JDBC & MySQL ===");
            System.out.println("1. Inicializar esquema de Base de Datos (DDL)");
            System.out.println("2. Insertar productos de prueba en BD");
            System.out.println("3. Mostrar Metadatos de la Base de Datos");
            System.out.println("4. Procesar un Pedido (Transaccion estricta con ROLLBACK/COMMIT)");
            System.out.println("0. Volver al menu principal");
            System.out.print("Elige una opcion: ");

            opcion = scanner.nextInt();
            scanner.nextLine(); // Limpiar buffer

            switch (opcion) {
                case 1:
                    System.out.println("\nEjecutando sentencias DDL en XAMPP...");
                    gestor.inicializarEsquema();
                    break;
                case 2:
                    System.out.println("\nInsertando productos de prueba en el catalogo...");
                    gestor.insertarProductoPrueba(new Producto(101, "Mando DualSense PS5", 69.99, 10, true));
                    gestor.insertarProductoPrueba(new Producto(102, "Acer Nitro V 15", 999.00, 5, true));
                    gestor.insertarProductoPrueba(new Producto(103, "Cable USB-C", 15.50, 50, true));
                    System.out.println("Productos insertados en base de datos.");
                    break;
                case 3:
                    System.out.println("\nRecuperando metadatos de la conexion...");
                    gestor.mostrarMetadatos();
                    break;
                case 4:
                    System.out.println("\nIniciando transaccion de pedido...");
                    List<Producto> carrito = new ArrayList<>();
                    List<Integer> cantidades = new ArrayList<>();
                    
                    // Simulación del carrito
                    carrito.add(new Producto(101, "Mando DualSense PS5", 69.99, 10, true));
                    cantidades.add(2); // Cantidad: 2
                    
                    carrito.add(new Producto(102, "Acer Nitro V 15", 999.00, 5, true));
                    cantidades.add(1); // Cantidad: 1
                    
                    gestor.procesarPedidoTransaccional("Roberto", carrito, cantidades);
                    break;
                case 0:
                    System.out.println("Volviendo al menu principal...");
                    break;
                default:
                    System.out.println("Opcion no valida.");
            }
        } while (opcion != 0);
    }

    /**
     * SUBMENÚ 3: Operaciones ORM con Hibernate / JPA para la Entrega 4
     */
    private static void mostrarSubmenuHibernate(Scanner scanner, GestorHibernate gestor) {
        int opcion;
        do {
            System.out.println("\n=== SUBMENU: ORM HIBERNATE / JPA (ENTREGA 4) ===");
            System.out.println("1. Inicializar datos de prueba ORM (Categorias, Productos y Clientes)");
            System.out.println("2. Registrar nuevo cliente");
            System.out.println("3. Listar catalogo de productos con categoria (HQL)");
            System.out.println("4. Listar clientes registrados (HQL)");
            System.out.println("5. Procesar un pedido ORM (Transaccion Hibernate 1:N y N:M)");
            System.out.println("6. Listar pedidos con relaciones Cliente y Productos (HQL)");
            System.out.println("7. Estadisticas de ventas por categoria (Consultas HQL/JPQL)");
            System.out.println("8. Probar ROLLBACK de transaccion ORM (Stock insuficiente)");
            System.out.println("0. Volver al menu principal");
            System.out.print("Elige una opcion: ");

            opcion = leerEntero(scanner);

            switch (opcion) {
                case 1:
                    System.out.println("\nInicializando entidades JPA y esquema en MySQL...");
                    gestor.inicializarDatosPrueba();
                    break;
                case 2:
                    System.out.print("Nombre completo del cliente: ");
                    String nombre = scanner.nextLine().trim();
                    System.out.print("Email del cliente: ");
                    String email = scanner.nextLine().trim();
                    System.out.print("Ciudad: ");
                    String ciudad = scanner.nextLine().trim();
                    gestor.registrarCliente(nombre, email, ciudad);
                    break;
                case 3:
                    gestor.listarCatalogoProductos();
                    break;
                case 4:
                    gestor.listarClientes();
                    break;
                case 5:
                    gestor.listarClientes();
                    System.out.print("\nIntroduce el ID del cliente: ");
                    int clienteId = leerEntero(scanner);

                    gestor.listarCatalogoProductos();
                    List<Integer> prodIds = new ArrayList<>();
                    List<Integer> cantidades = new ArrayList<>();

                    while (true) {
                        System.out.print("ID del producto a anadir (0 para finalizar carrito): ");
                        int prodId = leerEntero(scanner);
                        if (prodId == 0) {
                            break;
                        }
                        System.out.print("Cantidad: ");
                        int cant = leerEntero(scanner);
                        prodIds.add(prodId);
                        cantidades.add(cant);
                    }

                    if (!prodIds.isEmpty()) {
                        gestor.procesarPedidoORM(clienteId, prodIds, cantidades);
                    } else {
                        System.out.println("[AVISO] Pedido cancelado: carrito vacio.");
                    }
                    break;
                case 6:
                    gestor.listarPedidosConRelaciones();
                    break;
                case 7:
                    gestor.mostrarEstadisticasPorCategoria();
                    break;
                case 8:
                    gestor.probarRollbackStockInsuficiente();
                    break;
                case 0:
                    System.out.println("Volviendo al menu principal...");
                    break;
                default:
                    System.out.println("Opcion no valida.");
            }
        } while (opcion != 0);
    }
}