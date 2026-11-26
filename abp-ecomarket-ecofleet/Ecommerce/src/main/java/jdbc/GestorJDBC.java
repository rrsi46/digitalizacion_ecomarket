package jdbc;

import modelo.Producto;
import java.sql.*;
import java.util.List;

public class GestorJDBC {
    
    // Credenciales para XAMPP: localhost, puerto 3306, usuario root y sin contraseña
    // createDatabaseIfNotExist=true crea el esquema automáticamente si no existe
    private static final String URL = "jdbc:mysql://localhost:3306/ecomarket_db?createDatabaseIfNotExist=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // 1. Ejecución de sentencias DDL para inicializar la base de datos
    public void inicializarEsquema() {
        String createProductos = "CREATE TABLE IF NOT EXISTS productos (" +
                "id INT PRIMARY KEY, " +
                "nombre VARCHAR(100), " +
                "precio DECIMAL(10,2), " +
                "stock INT, " +
                "activo BOOLEAN)";

        String createPedidos = "CREATE TABLE IF NOT EXISTS pedidos (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "cliente VARCHAR(100))";

        String createDetalles = "CREATE TABLE IF NOT EXISTS detalle_pedidos (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "pedido_id INT, " +
                "producto_id INT, " +
                "cantidad INT, " +
                "precio_unitario DECIMAL(10,2), " +
                "FOREIGN KEY (pedido_id) REFERENCES pedidos(id), " +
                "FOREIGN KEY (producto_id) REFERENCES productos(id))";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createProductos);
            stmt.execute(createPedidos);
            stmt.execute(createDetalles);
            System.out.println("Esquema DDL inicializado en XAMPP correctamente.");
        } catch (SQLException e) {
            System.err.println("Error al inicializar las tablas: " + e.getMessage());
        }
    }
    
    // 2. Método auxiliar para insertar productos de prueba (DML simple)
    public void insertarProductoPrueba(Producto p) {
        String sql = "INSERT IGNORE INTO productos (id, nombre, precio, stock, activo) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, p.getId());
            pstmt.setString(2, p.getNombre());
            pstmt.setDouble(3, p.getPrecio());
            pstmt.setInt(4, p.getStock());
            pstmt.setBoolean(5, p.isActivo());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al insertar producto: " + e.getMessage());
        }
    }

    // 3. Extracción de Metadatos (DatabaseMetaData y ResultSetMetaData)
    public void mostrarMetadatos() {
        try (Connection conn = getConnection()) {
            DatabaseMetaData dbMetaData = conn.getMetaData();
            System.out.println("\n--- METADATOS DE LA BASE DE DATOS ---");
            System.out.println("Gestor de BD: " + dbMetaData.getDatabaseProductName());
            System.out.println("Version: " + dbMetaData.getDatabaseProductVersion());
            System.out.println("Driver JDBC: " + dbMetaData.getDriverName());

            System.out.println("\n--- METADATOS DE LA TABLA 'productos' ---");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM productos LIMIT 1")) {
                
                ResultSetMetaData rsMetaData = rs.getMetaData();
                int numColumnas = rsMetaData.getColumnCount();
                System.out.println("Numero de columnas: " + numColumnas);
                
                for (int i = 1; i <= numColumnas; i++) {
                    System.out.println("Columna " + i + ": " + rsMetaData.getColumnName(i) + 
                                       " - Tipo: " + rsMetaData.getColumnTypeName(i));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener metadatos: " + e.getMessage());
        }
    }

    // 4. Ejecución de DML con PreparedStatement y control estricto de transacciones
    public void procesarPedidoTransaccional(String cliente, List<Producto> carrito, List<Integer> cantidades) {
        String insertPedido = "INSERT INTO pedidos (cliente) VALUES (?)";
        String insertDetalle = "INSERT INTO detalle_pedidos (pedido_id, producto_id, cantidad, precio_unitario) VALUES (?, ?, ?, ?)";
        String updateStock = "UPDATE productos SET stock = stock - ? WHERE id = ? AND stock >= ?";

        Connection conn = null;
        try {
            conn = getConnection();
            
            // A. Iniciar transacción estricta desactivando el auto-commit
            conn.setAutoCommit(false); 

            // B. Crear el registro del pedido principal
            int pedidoId = -1;
            try (PreparedStatement pstmtPedido = conn.prepareStatement(insertPedido, Statement.RETURN_GENERATED_KEYS)) {
                pstmtPedido.setString(1, cliente);
                pstmtPedido.executeUpdate();
                try (ResultSet rs = pstmtPedido.getGeneratedKeys()) {
                    if (rs.next()) {
                        pedidoId = rs.getInt(1);
                    } else {
                        throw new SQLException("Fallo al generar el ID del pedido.");
                    }
                }
            }

            // C. Procesar artículos: actualizar stock y registrar detalle
            try (PreparedStatement pstmtDetalle = conn.prepareStatement(insertDetalle);
                 PreparedStatement pstmtStock = conn.prepareStatement(updateStock)) {

                for (int i = 0; i < carrito.size(); i++) {
                    Producto p = carrito.get(i);
                    int cantidadDeseada = cantidades.get(i);

                    // Actualizar stock de forma segura (falla si stock >= cantidadDeseada no se cumple)
                    pstmtStock.setInt(1, cantidadDeseada);
                    pstmtStock.setInt(2, p.getId());
                    pstmtStock.setInt(3, cantidadDeseada);
                    
                    int filasAfectadas = pstmtStock.executeUpdate();
                    if (filasAfectadas == 0) {
                        throw new SQLException("Stock insuficiente para el producto: " + p.getNombre());
                    }

                    // Insertar detalle del pedido
                    pstmtDetalle.setInt(1, pedidoId);
                    pstmtDetalle.setInt(2, p.getId());
                    pstmtDetalle.setInt(3, cantidadDeseada);
                    pstmtDetalle.setDouble(4, p.getPrecio());
                    pstmtDetalle.executeUpdate();
                }
            }

            // D. Si no hubo excepciones, confirmamos la transacción
            conn.commit(); 
            System.out.println("Pedido procesado con exito. Transaccion confirmada en la base de datos.");

        } catch (SQLException e) {
            System.err.println("Error en el procesamiento. Ejecutando ROLLBACK: " + e.getMessage());
            if (conn != null) {
                try {
                    // E. Revertir toda la transacción ante cualquier fallo
                    conn.rollback(); 
                } catch (SQLException ex) {
                    System.err.println("Error critico al intentar hacer rollback: " + ex.getMessage());
                }
            }
        } finally {
            if (conn != null) {
                try {
                    // Restaurar estado original de la conexión
                    conn.setAutoCommit(true); 
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}