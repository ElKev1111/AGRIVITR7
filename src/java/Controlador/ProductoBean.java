package Controlador;

import DAO.ProductoDAO;
import Modelo.Producto;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

@ManagedBean
@ViewScoped
public class ProductoBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private Producto producto = new Producto();
    private ProductoDAO productoDAO = new ProductoDAO();

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public List<Producto> getListaProductos() {
        try {
            return productoDAO.listar();
        } catch (SQLException e) {
            System.out.println("Error al listar los productos");
            return null;
        }
    }

    // NUEVO MÉTODO PARA OBTENER STOCK ACTUAL
    public int getStockActual(int idProducto) {
        try {
            Producto p = productoDAO.buscar(idProducto);
            return p != null ? p.getStock() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public String agregar() {
        try {
            // Validar campos requeridos
            if (producto.getNombreProducto() == null || producto.getNombreProducto().isEmpty()) {
                mostrarError("El nombre del producto es requerido");
                return null;
            }

            producto.setFechaIngreso(new Date());

            if (producto.getStock() < 0) {
                producto.setStock(0);
            }

            productoDAO.agregar(producto);
            producto = new Producto();

            mostrarExito(" Producto creado correctamente");
            return "HomeAdmin3?faces-redirect=true";

        } catch (SQLException e) {
            System.out.println("Error al insertar el producto: " + e.getMessage());
            mostrarError(" Error al crear producto: " + e.getMessage());
            return null;
        }
    }

    public void editar(Producto p) {
        this.producto = p;

    }

    public void actualizar() {
        try {
            productoDAO.actualizar(producto);

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", "Producto actualizado correctamente"));

            producto = new Producto(); // Limpiar el formulario

        } catch (Exception e) {
            System.out.println("Error al actualizar producto: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudo actualizar el producto: " + e.getMessage()));
        }
    }

    public void eliminar(Producto p) {
        try {
            productoDAO.eliminar(p);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", "Producto eliminado correctamente"));
        } catch (SQLException e) {
            System.out.println("Error al eliminar producto: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudo eliminar el producto: " + e.getMessage()));
        }
    }
    public List<Producto> getProductosPorProveedor(int idProveedor) {
    try {
        return productoDAO.listarPorProveedor(idProveedor);
    } catch (SQLException e) {
        mostrarError("Error al listar productos por proveedor");
        return null;
    }
}

    // Agregar métodos utilitarios para mensajes

    private void mostrarExito(String mensaje) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", mensaje));
    }

    private void mostrarError(String mensaje) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", mensaje));
    }
    
}
