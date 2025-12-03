package Controlador;

import DAO.VentasDAO;
import Modelo.Ventas;
import java.io.Serializable;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean(name = "ventasBean")
@ViewScoped
public class VentasBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final float IVA_PORCENTAJE = 0.015f; // 1.5 %

    private List<Ventas> listaVentas;
    private Ventas ventaSeleccionada;      // <-- venta que se muestra en el diálogo
    private VentasDAO ventasDAO;

    public VentasBean() {
        ventasDAO = new VentasDAO();
        cargarVentas();
    }
    // Método para listar las ventas
    public void cargarVentas() {
        listaVentas = ventasDAO.listar(); // Llama al DAO que trae las ventas
        System.out.println("Ventas cargadas: " + (listaVentas != null ? listaVentas.size() : 0));
    }

    // Se llama desde el botón "Ver resumen" en la tabla
    public void seleccionarVenta(Ventas v) {
        this.ventaSeleccionada = v;
    }

    // ===== Cálculos para la venta seleccionada (para el diálogo) =====

    // Total de productos SIN IVA (en tu BD guardas totalPagar sin IVA)
    public float getTotalProductosSeleccionado() {
        if (ventaSeleccionada == null) return 0f;
        return ventaSeleccionada.getTotalPagar();
    }

    // IVA 1.5 %
    public float getTotalIvaSeleccionado() {
        return getTotalProductosSeleccionado() * IVA_PORCENTAJE;
    }

    // Total con IVA
    public float getTotalConIvaSeleccionado() {
        return getTotalProductosSeleccionado() + getTotalIvaSeleccionado();
    }

    // ===== Getters / Setters =====

    public List<Ventas> getListaVentas() {
        return listaVentas;
    }

    public void setListaVentas(List<Ventas> listaVentas) {
        this.listaVentas = listaVentas;
    }

    public Ventas getVentaSeleccionada() {
        return ventaSeleccionada;
    }

    public void setVentaSeleccionada(Ventas ventaSeleccionada) {
        this.ventaSeleccionada = ventaSeleccionada;
    }
}
