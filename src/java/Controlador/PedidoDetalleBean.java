package Controlador;

import java.io.Serializable;
import java.util.Date; 
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import Modelo.PedidoProveedor;

@Named
@ViewScoped
public class PedidoDetalleBean implements Serializable {

    private int pedidoId;
    private PedidoProveedor pedido;

    public void cargarDetallePedido() {
        if (pedidoId != 0) {

        }
    }
    
    // Getters y Setters
    public int getPedidoId() { return pedidoId; }
    public void setPedidoId(int pedidoId) { this.pedidoId = pedidoId; }
    public PedidoProveedor getPedido() { return pedido; }
    public void setPedido(PedidoProveedor pedido) { this.pedido = pedido; }
}