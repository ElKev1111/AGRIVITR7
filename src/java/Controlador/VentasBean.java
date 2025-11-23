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

    private List<Ventas> listaVentas;
    private VentasDAO ventasDAO;

    public VentasBean() {
        ventasDAO = new VentasDAO();
        cargarVentas();
    }

    // Método para listar las ventas
    public void cargarVentas() {
        listaVentas = ventasDAO.listar(); // Llama al DAO que trae las ventas
        System.out.println("Ventas cargadas: " + listaVentas.size()); // Para depuración
    }

    // Getter y Setter
    public List<Ventas> getListaVentas() {
        return listaVentas;
    }

    public void setListaVentas(List<Ventas> listaVentas) {
        this.listaVentas = listaVentas;
    }
}
