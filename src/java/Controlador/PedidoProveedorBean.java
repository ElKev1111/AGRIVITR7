package Controlador;

import Modelo.PedidoProveedor;
import Modelo.Proveedor;
import Modelo.Producto;
import DAO.PedidoProveedorDAO;
import DAO.ProveedorDAO;
import DAO.ProductoDAO;
import java.io.Serializable;
import java.util.Date; 
import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

@ManagedBean(name = "pedidoProveedorBean")
@ViewScoped
public class PedidoProveedorBean implements Serializable {
    private static final long serialVersionUID =1L;

    private transient PedidoProveedorDAO pedidoDAO = new PedidoProveedorDAO();
    private transient ProveedorDAO proveedorDAO = new ProveedorDAO();
    private transient ProductoDAO productoDAO = new ProductoDAO();
    //private EmailService emailService = new EmailService(); 

    private List<PedidoProveedor> listaPedidos = new ArrayList<>();
    private List<Proveedor> listaProveedores = new ArrayList<>();
    private List<Producto> listaProductosPorProveedor = new ArrayList<>();
    private PedidoProveedor nuevoPedido = new PedidoProveedor();

    private int totalPedidos;
    private int totalPendientes;
    private int totalAceptados;
    private int totalRechazados;

    // Getters lazy para DAOs
    private PedidoProveedorDAO getPedidoDAO() {
        if (pedidoDAO == null) {
            pedidoDAO = new PedidoProveedorDAO();
        }
        return pedidoDAO;
    }

    private ProveedorDAO getProveedorDAO() {
        if (proveedorDAO == null) {
            proveedorDAO = new ProveedorDAO();
        }
        return proveedorDAO;
    }

    private ProductoDAO getProductoDAO() {
        if (productoDAO == null) {
            productoDAO = new ProductoDAO();
        }
        return productoDAO;
    }
   
    @PostConstruct
    public void init() {
        cargarProveedores();
        cargarPedidos();
    }

    public void cargarProveedores() {
        try {
            this.listaProveedores = getProveedorDAO().listar();
        } catch (SQLException e) {
            System.err.println("Error al cargar proveedores: " + e.getMessage());
        }
    }

    public void cargarPedidos() {
        try {
            this.listaPedidos = getPedidoDAO().listar();
            calcularEstadisticas();
        } catch (SQLException e) {
            System.err.println("Error al cargar pedidos: " + e.getMessage());
        }
    }

    private void calcularEstadisticas() {
        // Recalcula las estadísticas basadas en la listaPedidos actual
        this.totalPedidos = listaPedidos.size();
        this.totalPendientes = (int) listaPedidos.stream()
                .filter(p -> "ESPERA".equalsIgnoreCase(p.getEstado())) // Usa 'ESPERA' como el estado inicial
                .count();
        this.totalAceptados = (int) listaPedidos.stream()
                .filter(p -> "ACEPTADO".equalsIgnoreCase(p.getEstado()))
                .count();
        this.totalRechazados = (int) listaPedidos.stream()
                .filter(p -> "RECHAZADO".equalsIgnoreCase(p.getEstado()))
                .count();
    }

//    public void cargarProductosPorProveedor() {
//        FacesContext context = FacesContext.getCurrentInstance();
//        int idProveedorSeleccionado = nuevoPedido.getIdProveedor();
//
//        this.listaProductosPorProveedor.clear();
//        this.nuevoPedido.setIdProducto(0); 
//
//        if (idProveedorSeleccionado > 0) {
//            try {
//                // Requiere que ProductoDAO tenga un método listarPorProveedor(int idProveedor)
//                this.listaProductosPorProveedor = productoDAO.listarPorProveedor(idProveedorSeleccionado);
//            } catch (Exception e) {
//                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error BD",
//                        "Fallo al cargar productos: " + e.getMessage()));
//                e.printStackTrace();
//            }
//        }
//    }
     public void cargarProductosPorProveedor() {
        FacesContext context = FacesContext.getCurrentInstance();
        int idProveedorSeleccionado = nuevoPedido.getIdProveedor();
        
        this.listaProductosPorProveedor.clear();
        this.nuevoPedido.setIdProducto(0);

        if (idProveedorSeleccionado > 0) {
            try {
                // MÉTODO TEMPORAL - DEBES IMPLEMENTAR ProductoDAO.listarPorProveedor()
                this.listaProductosPorProveedor = getProductoDAO().listarPorProveedor(idProveedorSeleccionado);
            } catch (Exception e) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
                    "No se pudieron cargar los productos: " + e.getMessage()));
            }
        }
    }

    public String registrarNuevoPedido() {
        FacesContext context = FacesContext.getCurrentInstance();
        
        if (nuevoPedido.getIdProducto() <= 0 || nuevoPedido.getIdProveedor() <= 0) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validación", "Debe seleccionar Proveedor y Producto."));
            return null;
        }
        if (nuevoPedido.getCantidad() <= 0) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validación", "La Cantidad debe ser mayor a cero."));
            return null;
        }

        try {
            // **IMPORTANTE**: Estos métodos deben devolver el Modelo completo (Proveedor/Producto)
            Proveedor proveedorSeleccionado = getProveedorDAO().obtenerPorId(nuevoPedido.getIdProveedor());
            Producto productoSeleccionado = getProductoDAO().buscar(nuevoPedido.getIdProducto());

            if (proveedorSeleccionado == null || productoSeleccionado == null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Proveedor o Producto no encontrado."));
                return null;
            }

            // Asignar datos descriptivos y estado inicial
            nuevoPedido.setNombreProveedor(proveedorSeleccionado.getNombreProveedor());
            nuevoPedido.setNombreProducto(productoSeleccionado.getNombreProducto());

            if (nuevoPedido.getDescripcionPedido() == null || nuevoPedido.getDescripcionPedido().trim().isEmpty()) {
                String descripcionGenerada = "Pedido de " + nuevoPedido.getCantidad() + " de " + nuevoPedido.getNombreProducto() + " a " + nuevoPedido.getNombreProveedor();
                nuevoPedido.setDescripcionPedido(descripcionGenerada);
            }

            nuevoPedido.setEstado("ESPERA");

            // Llama al DAO para registrar el pedido
            if (getPedidoDAO().registrar(nuevoPedido)) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito",
                        "Pedido de " + nuevoPedido.getNombreProducto() + " creado."));

                // Limpiar y Recargar
                this.nuevoPedido = new PedidoProveedor();
                this.listaProductosPorProveedor.clear();
                cargarPedidos();

                return "pedidosProveedores.xhtml?faces-redirect=true"; 
            } else {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fallo", "No se pudo registrar el pedido (Error DAO)."));
                return null;
            }

        } catch (Exception e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_FATAL, "Error Grave", "Error al registrar el pedido: " + e.getMessage()));
            e.printStackTrace();
            return null;
        }
    }

    public void aceptarPedido(PedidoProveedor pedido) {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            pedido.setEstado("ACEPTADO");

            pedido.setFechaActualizacion(new Date());

            if (getPedidoDAO().actualizarEstado(pedido)) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito",
                        "Pedido ID " + pedido.getIdPedido() + " ACEPTADO."));
            } else {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error BD",
                        "No se pudo actualizar el estado del pedido."));
            }
            cargarPedidos(); // Recarga la lista y las estadísticas
        } catch (Exception e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_FATAL, "Error Grave",
                    "Fallo al aceptar el pedido: " + e.getMessage()));
        }
    }

    public void rechazarPedido(PedidoProveedor pedido) {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            pedido.setEstado("RECHAZADO");

            pedido.setFechaActualizacion(new Date());

            if (getPedidoDAO().actualizarEstado(pedido)) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Rechazado",
                        "Pedido ID " + pedido.getIdPedido() + " RECHAZADO."));
            } else {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error BD",
                        "No se pudo actualizar el estado del pedido."));
            }
            cargarPedidos(); 
        } catch (Exception e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_FATAL, "Error Grave",
                    "Fallo al rechazar el pedido: " + e.getMessage()));
        }
    }

   
    public int getTotalPedidos() {
        return totalPedidos;
    }

    public void setTotalPedidos(int totalPedidos) {
        this.totalPedidos = totalPedidos;
    }

    public int getTotalPendientes() {
        return totalPendientes;
    }

    public void setTotalPendientes(int totalPendientes) {
        this.totalPendientes = totalPendientes;
    }

    public int getTotalAceptados() {
        return totalAceptados;
    }

    public void setTotalAceptados(int totalAceptados) {
        this.totalAceptados = totalAceptados;
    }

    public int getTotalRechazados() {
        return totalRechazados;
    }

    public void setTotalRechazados(int totalRechazados) {
        this.totalRechazados = totalRechazados;
    }

    public List<PedidoProveedor> getListaPedidos() {
        return listaPedidos;
    }

    public void setListaPedidos(List<PedidoProveedor> listaPedidos) {
        this.listaPedidos = listaPedidos;
    }

    public List<Proveedor> getListaProveedores() {
        return listaProveedores;
    }

    public void setListaProveedores(List<Proveedor> listaProveedores) {
        this.listaProveedores = listaProveedores;
    }

    public PedidoProveedor getNuevoPedido() {
        return nuevoPedido;
    }

    public void setNuevoPedido(PedidoProveedor nuevoPedido) {
        this.nuevoPedido = nuevoPedido;
    }

    public List<Producto> getListaProductosPorProveedor() {
        return listaProductosPorProveedor;
    }

    public void setListaProductosPorProveedor(List<Producto> listaProductosPorProveedor) {
        this.listaProductosPorProveedor = listaProductosPorProveedor;
    }
}
