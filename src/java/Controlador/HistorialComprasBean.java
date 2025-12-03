package Controlador;

import DAO.VentasDAO;
import Modelo.Ventas;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

@ManagedBean(name = "historialComprasBean")
@ViewScoped
public class HistorialComprasBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Ventas> compras;
    private transient VentasDAO ventasDAO;

    @PostConstruct
    public void init() {
        // asegurar que nunca arranque null
        compras = new ArrayList<>();
        ventasDAO = new VentasDAO();
        cargarHistorial();
    }

    public void cargarHistorial() {
        try {
            // si por serialización quedó null, recrearlo
            if (ventasDAO == null) {
                ventasDAO = new VentasDAO();
            }

            UsuarioBean usuarioBean = FacesContext.getCurrentInstance().getApplication()
                    .evaluateExpressionGet(FacesContext.getCurrentInstance(),
                            "#{usuarioBean}", UsuarioBean.class);

            if (usuarioBean != null && usuarioBean.isAutenticado()
                    && usuarioBean.getUsuario() != null) {

                List<Ventas> desdeBD =
                        ventasDAO.listarPorUsuario(usuarioBean.getUsuario().getId());

                // si BD devuelve null, usar lista vacía
                compras = (desdeBD != null) ? desdeBD : new ArrayList<>();

            } else {
                compras = new ArrayList<>();
            }

        } catch (Exception e) {
            compras = new ArrayList<>();
        }
    }

    // IMPORTANTE: nunca retornar null
    public List<Ventas> getCompras() {
        if (compras == null) {
            compras = new ArrayList<>();
        }
        return compras;
    }
}
