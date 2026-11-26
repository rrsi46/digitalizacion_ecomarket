package hibernate;

import modelo.Categoria;
import modelo.Cliente;
import modelo.Pedido;
import modelo.Producto;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestor de persistencia ORM con Hibernate / JPA para la Entrega 4.
 * Sustituye las consultas SQL manuales de JDBC por entidades JPA, relaciones
 * 1:N / N:M, control transaccional ORM y consultas HQL/JPQL de agregacion.
 */
public class GestorHibernate {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Inicializa categorias, productos categorizados y clientes de prueba en ecomarket_db.
     * Reutiliza los productos existentes (IDs 101-103 de Entrega 3) asignandoles categoria
     * e incorpora nuevos articulos para enriquecer las estadisticas por categoria.
     */
    public void inicializarDatosPrueba() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();

                Categoria catPerifericos = obtenerOCrearCategoria(session, "Perifericos", "Mandos, teclados y accesorios gaming");
                Categoria catPortatiles = obtenerOCrearCategoria(session, "Portatiles", "Ordenadores portatiles de alto rendimiento");
                Categoria catCables = obtenerOCrearCategoria(session, "Cables", "Conectividad y cables de carga o video");
                Categoria catMonitores = obtenerOCrearCategoria(session, "Monitores", "Pantallas 4K y de alta frecuencia");
                Categoria catComponentes = obtenerOCrearCategoria(session, "Componentes", "Almacenamiento SSD y hardware interno");

                crearOActualizarProducto(session, 101, "Mando DualSense PS5", 69.99, 10, true, catPerifericos);
                crearOActualizarProducto(session, 102, "Acer Nitro V 15", 999.00, 5, true, catPortatiles);
                crearOActualizarProducto(session, 103, "Cable USB-C", 15.50, 50, true, catCables);
                crearOActualizarProducto(session, 104, "Teclado Mecanico RGB", 89.99, 20, true, catPerifericos);
                crearOActualizarProducto(session, 105, "Monitor 27 Pulgadas 4K", 349.90, 8, true, catMonitores);
                crearOActualizarProducto(session, 106, "Disco SSD 1TB NVMe", 99.99, 15, true, catComponentes);

                obtenerOCrearCliente(session, "Roberto Seminario", "roberto@ecomarket.es", "Madrid");
                obtenerOCrearCliente(session, "Laura Gomez", "laura@ecomarket.es", "Majadahonda");
                obtenerOCrearCliente(session, "Carlos Perez", "carlos@ecomarket.es", "Las Rozas");

                tx.commit();
                System.out.println("[EXITO] Entorno ORM inicializado: categorias, productos y clientes listos en MySQL.");
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
                System.err.println("[ERROR] Al inicializar datos ORM: " + e.getMessage());
            }
        }
    }

    private Categoria obtenerOCrearCategoria(Session session, String nombre, String descripcion) {
        Query<Categoria> q = session.createQuery("FROM Categoria c WHERE c.nombre = :nombre", Categoria.class);
        q.setParameter("nombre", nombre);
        Categoria cat = q.uniqueResult();
        if (cat == null) {
            cat = new Categoria(nombre, descripcion);
            session.persist(cat);
        }
        return cat;
    }

    private void crearOActualizarProducto(Session session, int id, String nombre, double precio,
                                          int stock, boolean activo, Categoria categoria) {
        Producto existente = session.get(Producto.class, id);
        if (existente == null) {
            session.persist(new Producto(id, nombre, precio, stock, activo, categoria));
        } else {
            if (existente.getCategoria() == null) {
                existente.setCategoria(categoria);
            }
            if (existente.getStock() <= 0) {
                existente.setStock(stock);
                existente.setActivo(true);
            }
        }
    }

    private void obtenerOCrearCliente(Session session, String nombre, String email, String ciudad) {
        Query<Cliente> q = session.createQuery("FROM Cliente c WHERE c.email = :email", Cliente.class);
        q.setParameter("email", email);
        if (q.uniqueResult() == null) {
            session.persist(new Cliente(nombre, email, ciudad));
        }
    }

    /**
     * Registra un nuevo cliente en la base de datos utilizando Hibernate.
     */
    public void registrarCliente(String nombre, String email, String ciudad) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                Query<Long> q = session.createQuery("SELECT COUNT(c) FROM Cliente c WHERE c.email = :email", Long.class);
                q.setParameter("email", email);
                if (q.uniqueResult() > 0) {
                    System.out.println("[AVISO] Ya existe un cliente registrado con el email: " + email);
                    return;
                }

                tx = session.beginTransaction();
                Cliente nuevo = new Cliente(nombre, email, ciudad);
                session.persist(nuevo);
                tx.commit();
                System.out.println("[EXITO] Cliente registrado con ID: " + nuevo.getId() + " (" + nuevo.getNombre() + ")");
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
                System.err.println("[ERROR] Al registrar cliente: " + e.getMessage());
            }
        }
    }

    /**
     * Lista todos los clientes registrados mediante HQL.
     */
    public void listarClientes() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Cliente> clientes = session.createQuery("FROM Cliente c ORDER BY c.id", Cliente.class).list();
            System.out.println("\n--- LISTADO DE CLIENTES (ORM HQL) ---");
            if (clientes.isEmpty()) {
                System.out.println("[AVISO] No hay clientes registrados. Ejecuta primero la opcion 1.");
                return;
            }
            for (Cliente c : clientes) {
                System.out.println(c);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Al listar clientes: " + e.getMessage());
        }
    }

    /**
     * Lista el catalogo completo de productos junto con su categoria (LEFT JOIN FETCH).
     */
    public void listarCatalogoProductos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT p FROM Producto p LEFT JOIN FETCH p.categoria ORDER BY p.id";
            List<Producto> productos = session.createQuery(hql, Producto.class).list();
            System.out.println("\n--- CATALOGO DE PRODUCTOS EN MYSQL (ORM HQL) ---");
            if (productos.isEmpty()) {
                System.out.println("[AVISO] El catalogo esta vacio.");
                return;
            }
            for (Producto p : productos) {
                System.out.println(p);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Al listar productos: " + e.getMessage());
        }
    }

    /**
     * Procesa un pedido de forma transaccional usando entidades JPA y Hibernate.
     * Actualiza el stock mediante dirty checking y persiste las relaciones Cliente (1:N)
     * y Producto (N:M). Si algun producto no tiene stock suficiente, ejecuta rollback().
     */
    public boolean procesarPedidoORM(int clienteId, List<Integer> productoIds, List<Integer> cantidades) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();

                Cliente cliente = session.get(Cliente.class, clienteId);
                if (cliente == null) {
                    throw new IllegalArgumentException("No existe ningun cliente con ID: " + clienteId);
                }
                if (productoIds.isEmpty()) {
                    throw new IllegalArgumentException("El carrito esta vacio.");
                }

                Pedido pedido = new Pedido(cliente);

                for (int i = 0; i < productoIds.size(); i++) {
                    int prodId = productoIds.get(i);
                    int cantidad = cantidades.get(i);

                    Producto prod = session.get(Producto.class, prodId);
                    if (prod == null) {
                        throw new IllegalArgumentException("Producto no encontrado con ID: " + prodId);
                    }
                    if (cantidad <= 0) {
                        throw new IllegalArgumentException("Cantidad invalida para " + prod.getNombre());
                    }
                    if (!prod.isActivo() || prod.getStock() < cantidad) {
                        throw new IllegalStateException("Stock insuficiente para '" + prod.getNombre()
                                + "' (disponible: " + prod.getStock() + ", solicitado: " + cantidad + ")");
                    }

                    // Actualizacion automatica en BD gracias al dirty checking de Hibernate
                    int nuevoStock = prod.getStock() - cantidad;
                    prod.setStock(nuevoStock);
                    if (nuevoStock == 0) {
                        prod.setActivo(false);
                    }

                    // Asociacion N:M (@ManyToMany) en la tabla intermedia pedido_productos
                    for (int u = 0; u < cantidad; u++) {
                        pedido.addProducto(prod);
                    }
                }

                cliente.addPedido(pedido);
                session.persist(pedido);
                tx.commit();

                System.out.printf("[EXITO] Pedido #%d confirmado para %s | Total: %.2f euros (%d unidades)%n",
                        pedido.getId(), cliente.getNombre(), pedido.calcularTotal(), pedido.getProductos().size());
                return true;

            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
                System.out.println("[ROLLBACK] Transaccion cancelada: " + e.getMessage());
                return false;
            }
        }
    }

    /**
     * Consulta HQL que recupera los pedidos con su cliente (1:N) y sus productos (N:M).
     */
    public void listarPedidosConRelaciones() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT ped FROM Pedido ped "
                    + "JOIN FETCH ped.cliente c "
                    + "ORDER BY ped.id";
            List<Pedido> pedidos = session.createQuery(hql, Pedido.class).list();

            System.out.println("\n--- PEDIDOS REGISTRADOS (RELACION 1:N Cliente-Pedido y N:M Pedido-Producto) ---");
            if (pedidos.isEmpty()) {
                System.out.println("[AVISO] Todavia no hay pedidos ORM registrados.");
                return;
            }

            for (Pedido ped : pedidos) {
                System.out.printf("Pedido #%-3d | Fecha: %s | Cliente: %s (%s) | Estado: %s%n",
                        ped.getId(),
                        ped.getFecha().format(FORMATO_FECHA),
                        ped.getCliente().getNombre(),
                        ped.getCliente().getCiudad(),
                        ped.getEstado());

                // Agrupamos lineas de la coleccion N:M para mostrar unidades y subtotal
                Map<Integer, Integer> conteo = new LinkedHashMap<>();
                Map<Integer, Producto> refProd = new LinkedHashMap<>();
                for (Producto prod : ped.getProductos()) {
                    conteo.merge(prod.getId(), 1, Integer::sum);
                    refProd.putIfAbsent(prod.getId(), prod);
                }
                for (Map.Entry<Integer, Integer> entry : conteo.entrySet()) {
                    Producto prod = refProd.get(entry.getKey());
                    int uds = entry.getValue();
                    String cat = prod.getCategoria() != null ? prod.getCategoria().getNombre() : "Sin categoria";
                    System.out.printf("   -> %-26s [%-11s] | %d ud(s) x %7.2f = %8.2f euros%n",
                            prod.getNombre(), cat, uds, prod.getPrecio(), uds * prod.getPrecio());
                }
                System.out.printf("   Total Pedido #%d: %.2f euros%n", ped.getId(), ped.calcularTotal());
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Al consultar pedidos ORM: " + e.getMessage());
        }
    }

    /**
     * Ejecuta consultas complejas HQL/JPQL con JOIN, GROUP BY, HAVING y funciones de agregacion
     * para obtener estadisticas de ventas por categoria, ranking de clientes y top productos.
     */
    public void mostrarEstadisticasPorCategoria() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // 1. Consulta HQL principal: estadisticas agregadas por categoria
            String hqlCategorias = "SELECT cat.nombre, COUNT(DISTINCT ped.id), COUNT(prod.id), "
                    + "COALESCE(SUM(prod.precio), 0.0), COALESCE(AVG(prod.precio), 0.0), MAX(prod.precio) "
                    + "FROM Pedido ped "
                    + "JOIN ped.productos prod "
                    + "JOIN prod.categoria cat "
                    + "GROUP BY cat.id, cat.nombre "
                    + "ORDER BY SUM(prod.precio) DESC";

            List<Object[]> statsCategorias = session.createQuery(hqlCategorias, Object[].class).list();

            System.out.println("\n=== ESTADISTICAS DE VENTAS POR CATEGORIA (HQL/JPQL) ===");
            if (statsCategorias.isEmpty()) {
                System.out.println("[AVISO] No hay ventas registradas todavia. Procesa al menos un pedido (opcion 5).");
                return;
            }

            System.out.printf("%-15s | %-7s | %-9s | %-14s | %-13s | %-11s%n",
                    "CATEGORIA", "PEDIDOS", "UNIDADES", "TOTAL VENTAS", "PRECIO MEDIO", "PRECIO MAX");
            System.out.println("--------------------------------------------------------------------------------");
            for (Object[] fila : statsCategorias) {
                System.out.printf("%-15s | %-7d | %-9d | %11.2f EUR | %10.2f EUR | %8.2f EUR%n",
                        fila[0],
                        ((Number) fila[1]).longValue(),
                        ((Number) fila[2]).longValue(),
                        ((Number) fila[3]).doubleValue(),
                        ((Number) fila[4]).doubleValue(),
                        ((Number) fila[5]).doubleValue());
            }

            // 2. Consulta HQL: ranking de clientes con GROUP BY y HAVING
            String hqlClientes = "SELECT c.nombre, c.ciudad, COUNT(DISTINCT ped.id), COUNT(prod.id), SUM(prod.precio) "
                    + "FROM Cliente c "
                    + "JOIN c.pedidos ped "
                    + "JOIN ped.productos prod "
                    + "GROUP BY c.id, c.nombre, c.ciudad "
                    + "HAVING SUM(prod.precio) > 0 "
                    + "ORDER BY SUM(prod.precio) DESC";

            List<Object[]> statsClientes = session.createQuery(hqlClientes, Object[].class).list();
            System.out.println("\n--- RANKING DE CLIENTES POR FACTURACION (HQL GROUP BY + HAVING) ---");
            System.out.printf("%-22s | %-13s | %-7s | %-9s | %-14s%n",
                    "CLIENTE", "CIUDAD", "PEDIDOS", "UNIDADES", "GASTO TOTAL");
            System.out.println("----------------------------------------------------------------------------");
            for (Object[] fila : statsClientes) {
                System.out.printf("%-22s | %-13s | %-7d | %-9d | %11.2f EUR%n",
                        fila[0],
                        fila[1],
                        ((Number) fila[2]).longValue(),
                        ((Number) fila[3]).longValue(),
                        ((Number) fila[4]).doubleValue());
            }

            // 3. Consulta HQL: productos mas vendidos con stock actual
            String hqlProductos = "SELECT prod.id, prod.nombre, cat.nombre, COUNT(ped.id), SUM(prod.precio), prod.stock "
                    + "FROM Pedido ped "
                    + "JOIN ped.productos prod "
                    + "JOIN prod.categoria cat "
                    + "GROUP BY prod.id, prod.nombre, cat.nombre, prod.stock "
                    + "ORDER BY COUNT(ped.id) DESC, SUM(prod.precio) DESC";

            List<Object[]> statsProductos = session.createQuery(hqlProductos, Object[].class).list();
            System.out.println("\n--- TOP PRODUCTOS VENDIDOS Y STOCK RESTANTE (HQL MULTI-JOIN) ---");
            System.out.printf("%-4s | %-25s | %-13s | %-8s | %-13s | %-6s%n",
                    "ID", "PRODUCTO", "CATEGORIA", "VENDIDOS", "INGRESOS", "STOCK");
            System.out.println("----------------------------------------------------------------------------------");
            for (Object[] fila : statsProductos) {
                System.out.printf("%-4d | %-25s | %-13s | %-8d | %10.2f EUR | %-6d%n",
                        ((Number) fila[0]).intValue(),
                        fila[1],
                        fila[2],
                        ((Number) fila[3]).longValue(),
                        ((Number) fila[4]).doubleValue(),
                        ((Number) fila[5]).intValue());
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Al ejecutar consultas HQL de estadisticas: " + e.getMessage());
        }
    }

    /**
     * Demostracion controlada de ROLLBACK en Hibernate cuando falla la validacion de stock
     * en el segundo articulo de un carrito tras haber modificado el primero en memoria.
     */
    public void probarRollbackStockInsuficiente() {
        System.out.println("\n--- PRUEBA DE ROLLBACK TRANSACCIONAL ORM ---");
        int clienteId;
        int stockAntes;
        long pedidosAntes;
        String nombreP1;
        String nombreP2;
        int stockP2;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Producto p1 = session.get(Producto.class, 103);
            Producto p2 = session.get(Producto.class, 102);
            Cliente cli = session.createQuery("FROM Cliente c ORDER BY c.id", Cliente.class)
                    .setMaxResults(1).uniqueResult();

            if (p1 == null || p2 == null || cli == null) {
                System.out.println("[AVISO] Ejecuta primero la opcion 1 para inicializar los datos de prueba.");
                return;
            }

            clienteId = cli.getId();
            stockAntes = p1.getStock();
            nombreP1 = p1.getNombre();
            nombreP2 = p2.getNombre();
            stockP2 = p2.getStock();
            pedidosAntes = session.createQuery("SELECT COUNT(p) FROM Pedido p", Long.class).uniqueResult();
        } catch (Exception e) {
            System.err.println("[ERROR] Al preparar prueba de rollback: " + e.getMessage());
            return;
        }

        int cantidadExcesiva = stockP2 + 50;
        System.out.printf("Estado previo -> Stock %s (ID 103): %d | Pedidos ORM totales: %d%n",
                nombreP1, stockAntes, pedidosAntes);
        System.out.printf("Intentando comprar 1x %s + %dx %s (supera stock disponible de %d)...%n",
                nombreP1, cantidadExcesiva, nombreP2, stockP2);

        procesarPedidoORM(clienteId, List.of(103, 102), List.of(1, cantidadExcesiva));

        try (Session verificacion = HibernateUtil.getSessionFactory().openSession()) {
            Producto p1Despues = verificacion.get(Producto.class, 103);
            long pedidosDespues = verificacion.createQuery("SELECT COUNT(p) FROM Pedido p", Long.class).uniqueResult();
            System.out.printf("Estado posterior al ROLLBACK -> Stock %s (ID 103): %d | Pedidos ORM totales: %d%n",
                    nombreP1, p1Despues.getStock(), pedidosDespues);
        } catch (Exception e) {
            System.err.println("[ERROR] Al verificar estado posterior al rollback: " + e.getMessage());
        }
    }
}
