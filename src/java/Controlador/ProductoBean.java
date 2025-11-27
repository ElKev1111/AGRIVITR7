package Controlador;

import DAO.ProductoDAO;
import DAO.ProveedorDAO;
import Modelo.Producto;
import Modelo.Proveedor;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
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

    private ProveedorDAO proveedorDAO = new ProveedorDAO();
    private List<Proveedor> listaProveedores;
    private Integer idProveedorSeleccionado;

    @PostConstruct
    public void init() {
        try {
            listaProveedores = proveedorDAO.listarActivos();
            System.out.println(">>> Proveedores cargados: " + listaProveedores.size());
        } catch (SQLException e) {
            System.out.println("Error al cargar proveedores: " + e.getMessage());
            mostrarError("Error al cargar proveedores: " + e.getMessage());
        }
    }

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
            // Validar campos requeridos básicos
            if (producto.getNombreProducto() == null || producto.getNombreProducto().isEmpty()) {
                mostrarError("El nombre del producto es requerido");
                return null;
            }
            if (idProveedorSeleccionado == null) {
                mostrarError("Debe seleccionar un proveedor");
                return null;
            }

            // 🔹 Fecha de ingreso
            producto.setFechaIngreso(new Date());

            // 🔹 Cargar proveedor desde la BD
            Proveedor proveedor = proveedorDAO.buscar(idProveedorSeleccionado); // o buscarPorId, según tu DAO
            if (proveedor == null) {
                mostrarError("No se encontró el proveedor seleccionado");
                return null;
            }

            // 🔹 Asignar datos de proveedor al producto
            producto.setIdProveedor(proveedor.getIdProveedor());
            producto.setNombreProveedor(proveedor.getNombreProveedor());

            // Si el stock viene nulo o negativo, normalizamos
            if (producto.getStock() < 0) {
                producto.setStock(0);
            }

            productoDAO.agregar(producto);
            producto = new Producto();
            idProveedorSeleccionado = null;   // limpiamos la selección

            mostrarExito("Producto creado correctamente");
            return "HomeAdmin3?faces-redirect=true";

        } catch (SQLException e) {
            System.out.println("Error al insertar el producto: " + e.getMessage());
            mostrarError("Error al crear producto: " + e.getMessage());
            return null;
        }
    }

    public void editar(Producto p) {
        this.producto = p;

    }

    public void actualizar() {
    try {
        System.out.println("STOCK ANTES DE ACTUALIZAR = " + producto.getStock());

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
            // 1. Verificar si tiene movimientos de inventario
            if (productoDAO.tieneMovimientosInventario(p.getIdProducto())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                                "Aviso",
                                "No se puede eliminar el producto '" + p.getNombreProducto()
                                + "' porque tiene movimientos de inventario registrados."));
                return; // Salimos sin eliminar
            }

            // 2. Si no tiene movimientos, sí se elimina
            productoDAO.eliminar(p);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Éxito", "Producto eliminado correctamente"));

        } catch (SQLException e) {
            System.out.println("Error al eliminar producto: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", "No se pudo eliminar el producto: " + e.getMessage()));
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

    private void mostrarExito(String mensaje) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", mensaje));
    }

    private void mostrarError(String mensaje) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", mensaje));
    }

    public Integer getIdProveedorSeleccionado() {
        return idProveedorSeleccionado;
    }

    public void setIdProveedorSeleccionado(Integer idProveedorSeleccionado) {
        this.idProveedorSeleccionado = idProveedorSeleccionado;
    }

    public List<Proveedor> getListaProveedores() {
    try {
        listaProveedores = proveedorDAO.listar();
    } catch (SQLException e) {
        mostrarError("Error al cargar proveedores: " + e.getMessage());
    }
    return listaProveedores;
}


    

}
