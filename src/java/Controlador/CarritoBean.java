package Controlador;

import Modelo.Producto;
import Modelo.CarritoItem;
import DAO.ProductoDAO;
import DAO.VentasDAO;
import DAO.MovInventarioDAO;
import Modelo.MovInventario;
import Modelo.Ventas;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.application.FacesMessage;
import java.time.LocalDateTime;

@ManagedBean
@SessionScoped
public class CarritoBean implements Serializable {

    private List<CarritoItem> items = new ArrayList<>();
    private int cantidadTemporal = 1;
    private Producto productoTemporal;
    private CarritoItem itemEdicion;

    private transient ProductoDAO productoDAO = new ProductoDAO();
    private transient VentasDAO ventasDAO = new VentasDAO();
    private transient MovInventarioDAO movInventarioDAO = new MovInventarioDAO();
    private boolean simulacionPublicaInicializada;

    public void prepararSimulacionPublica() {
        // Restablece el carrito cuando se entra por primera vez al dashboard público
        // para que la simulación termine al recargar o cerrar la vista.
        if (!usuarioAutenticado() && !FacesContext.getCurrentInstance().isPostback() && !simulacionPublicaInicializada) {
            items.clear();
            simulacionPublicaInicializada = true;
        }
    }

    // Método para preparar el producto temporal
    public void prepararAgregarProducto(Producto producto) {
        this.productoTemporal = producto;
        this.cantidadTemporal = 1;
        System.out.println("✅ Producto preparado: " + producto.getNombreProducto());
    }

    // Método para agregar desde el modal
    public void agregarProductoModal() {
        if (productoTemporal != null && cantidadTemporal > 0) {
            agregarProductoConCantidad(productoTemporal, cantidadTemporal);
            System.out.println("✅ Producto agregado: " + productoTemporal.getNombreProducto() + ", Cantidad: " + cantidadTemporal);
            productoTemporal = null;
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                "No se pudo agregar el producto al carrito."));
        }
    }

    // Método mejorado con cantidad específica
    public void agregarProductoConCantidad(Producto producto, int cantidad) {
        boolean encontrado = false;
        
        for (CarritoItem item : items) {
            if (item.getProducto().getIdProducto() == producto.getIdProducto()) {
                item.setCantidad(item.getCantidad() + cantidad); 
                encontrado = true;
                break;
            }
        }
        
        if (!encontrado) {
            items.add(new CarritoItem(producto, cantidad));
        }
        
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_INFO, "¡Producto Añadido!", 
            cantidad + " unidad(es) de " + producto.getNombreProducto() + " agregado al carrito."));
    }

    // Eliminar item completo
    public void eliminarItem(CarritoItem item) {
        String nombreProducto = item.getProducto().getNombreProducto();
        items.remove(item);
        
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_WARN, "Item Eliminado", 
            nombreProducto + " eliminado del carrito."));
    }

    // MÉTODOS PARA EDITAR CANTIDAD
    public void prepararEditarCantidad(CarritoItem item) {
        this.itemEdicion = item;
        this.cantidadTemporal = item.getCantidad();
        System.out.println("📝 Preparando edición: " + item.getProducto().getNombreProducto() + 
                          ", Cantidad actual: " + item.getCantidad());
    }
    
    public void actualizarCantidad() {
        if (itemEdicion != null && cantidadTemporal > 0) {
            int cantidadAnterior = itemEdicion.getCantidad();
            itemEdicion.setCantidad(cantidadTemporal);
            
            System.out.println("✅ Cantidad actualizada: " + itemEdicion.getProducto().getNombreProducto() + 
                              " de " + cantidadAnterior + " a " + cantidadTemporal);
            
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Cantidad Actualizada", 
                itemEdicion.getProducto().getNombreProducto() + " actualizado a " + cantidadTemporal + " unidades."));
            
            // Limpiar después de actualizar
            itemEdicion = null;
        }
    }

    // Método de pago
    public void procederAlPago() {
        if (!usuarioAutenticado()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Inicia sesión", "Debes iniciar sesión para completar el pago."));
            return;
        }
        if (isVacio()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Carrito Vacío",
                "No puedes proceder al pago con el carrito vacío."));
            return;
        }

        procesarPago();
    }
    
    // Cálculos
    public float getTotalCompra() {
        float total = 0;
        for (CarritoItem item : items) {
            total += item.getSubtotal();
        }
        return total;
    }
    
    public int getTotalItems() {
        return items.stream().mapToInt(CarritoItem::getCantidad).sum();
    }
    
    public boolean isVacio() {
        return items.isEmpty();
    }

    // Getters y Setters
    public List<CarritoItem> getItems() {
        return items;
    }
    
    public int getCantidadTemporal() {
        return cantidadTemporal;
    }
    
    public void setCantidadTemporal(int cantidadTemporal) {
        this.cantidadTemporal = cantidadTemporal;
    }
    
    public Producto getProductoTemporal() {
        return productoTemporal;
    }
    
    public void setProductoTemporal(Producto productoTemporal) {
        this.productoTemporal = productoTemporal;
    }
    
    public CarritoItem getItemEdicion() {
        return itemEdicion;
    }
    
    public void setItemEdicion(CarritoItem itemEdicion) {
        this.itemEdicion = itemEdicion;
    }

    private UsuarioBean obtenerUsuarioBean() {
        return FacesContext.getCurrentInstance().getApplication()
                .evaluateExpressionGet(FacesContext.getCurrentInstance(), "#{usuarioBean}", UsuarioBean.class);
    }

    private boolean usuarioAutenticado() {
        UsuarioBean usuarioBean = obtenerUsuarioBean();
        return usuarioBean != null && usuarioBean.isAutenticado();
    }

    private void redirigirALogin() {
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect("login.xhtml");
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Sesión requerida",
                        "Inicia sesión para continuar."));
        }
    }

    private void procesarPago() {
        UsuarioBean usuarioBean = obtenerUsuarioBean();

        try {
            for (CarritoItem item : items) {
                Producto productoDB = productoDAO.buscar(item.getProducto().getIdProducto());
                item.setProducto(productoDB);

                if (productoDB == null) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Producto no encontrado",
                                    "No se pudo encontrar el producto en inventario."));
                    return;
                }

                if (productoDB.getStock() < item.getCantidad()) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_WARN, "Stock insuficiente",
                                    "No hay stock suficiente de " + productoDB.getNombreProducto()));
                    return;
                }

                int stockNuevo = productoDB.getStock() - item.getCantidad();
                productoDAO.actualizarStock(productoDB.getIdProducto(), stockNuevo);

                MovInventario mov = new MovInventario();
                mov.setTipoString("Salida");
                mov.setCantidadSalida(item.getCantidad());
                mov.setFechaSalida(new java.util.Date());
                mov.setTipoSalida("Venta");
                mov.setDescripcion("Venta de " + item.getCantidad() + " x " + productoDB.getNombreProducto());
                mov.setCliente(usuarioBean.getUsuario().getNombre());
                mov.setPrecioVenta((double) item.getProducto().getPrecioVenta());
                mov.setIdProducto(productoDB.getIdProducto());
                mov.setStockAnterior(productoDB.getStock());
                mov.setStockNuevo(stockNuevo);
                movInventarioDAO.registrarMovimiento(mov);

                Ventas venta = new Ventas();
                venta.setFechaVenta(LocalDateTime.now());
                venta.setProducto(productoDB);
                venta.setUsuario(usuarioBean.getUsuario());
                venta.setCantidad(item.getCantidad());
                venta.setTotalPagar(item.getSubtotal());
                ventasDAO.registrar(venta);
            }

            items.clear();

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Pago registrado",
                            "Se simuló el pago y se actualizó el inventario."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error en pago",
                            "No se pudo procesar el pago: " + e.getMessage()));
        }
    }
}