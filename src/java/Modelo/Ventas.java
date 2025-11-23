package Modelo;

import java.time.LocalDateTime;

public class Ventas {

    private int idVenta;
    private LocalDateTime fechaVenta;
    private Producto producto;
    private Usuario usuario;
    private int cantidad;
    private float totalPagar;

    // Constructor vacío
    public Ventas() {
    }

    public Ventas(int idVenta, LocalDateTime fechaVenta, Producto producto, Usuario usuario, int cantidad, float totalPagar) {
        this.idVenta = idVenta;
        this.fechaVenta = fechaVenta;
        this.producto = producto;
        this.usuario = usuario;
        this.cantidad = cantidad;
        this.totalPagar = totalPagar;
    }

    public int getIdVenta() {
        return idVenta;
    }

    public LocalDateTime getFechaVenta() {
        return fechaVenta;
    }

    public Producto getProducto() {
        return producto;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public int getCantidad() {
        return cantidad;
    }

    public float getTotalPagar() {
        return totalPagar;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public void setFechaVenta(LocalDateTime fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public void setTotalPagar(float totalPagar) {
        this.totalPagar = totalPagar;
    }

   
}
