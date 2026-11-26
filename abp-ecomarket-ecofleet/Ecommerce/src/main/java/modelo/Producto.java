package modelo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA y modelo comun de Producto para ficheros, JDBC y ORM Hibernate.
 * Mantiene relacion N:1 con Categoria y relacion N:M (@ManyToMany) con Pedido.
 */
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    private int id;

    @Column(nullable = false, length = 100)
    private String nombre; // Máximo 30 caracteres (60 bytes en UTF-16)

    @Column(nullable = false)
    private double precio;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private boolean activo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    @JsonIgnore
    private Categoria categoria;

    @ManyToMany(mappedBy = "productos", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Pedido> pedidos = new ArrayList<>();

    // CONSTRUCTOR VACÍO OBLIGATORIO PARA JACKSON (JSON) Y JPA/HIBERNATE
    public Producto() {
    }

    public Producto(int id, String nombre, double precio, int stock, boolean activo) {
        this.id = id;
        this.nombre = nombre != null ? nombre : "";
        this.precio = precio;
        this.stock = stock;
        this.activo = activo;
    }

    public Producto(int id, String nombre, double precio, int stock, boolean activo, Categoria categoria) {
        this(id, nombre, precio, stock, activo);
        this.categoria = categoria;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public List<Pedido> getPedidos() { return pedidos; }
    public void setPedidos(List<Pedido> pedidos) { this.pedidos = pedidos; }

    @Override
    public String toString() {
        String base = String.format("ID: %-3d | Nombre: %-30s | Precio: %8.2f euros | Stock: %-4d | Activo: %b",
                id, nombre, precio, stock, activo);
        if (categoria != null) {
            return base + " | Categoria: " + categoria.getNombre();
        }
        return base;
    }
}