package Controlador;

import Modelo.Producto;
import Modelo.CarritoItem;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.application.FacesMessage;

@ManagedBean
@SessionScoped
public class CarritoBean implements Serializable {

    private List<CarritoItem> items = new ArrayList<>();
    private int cantidadTemporal = 1;
    private Producto productoTemporal;
    private CarritoItem itemEdicion;

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
        if (isVacio()) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Carrito Vacío", 
                "No puedes proceder al pago con el carrito vacío."));
            return;
        }
        
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Procesando Pago", 
            "Redirigiendo a la página de pagos... Total: $" + getTotalCompra()));
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
}