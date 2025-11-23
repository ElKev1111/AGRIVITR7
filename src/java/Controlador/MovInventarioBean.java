package Controlador;

import DAO.MovInventarioDAO;
import DAO.ProductoDAO;
import Servicio.ServicioInventario;
import Modelo.MovInventario;
import Modelo.Producto;
import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;
import javax.faces.bean.ViewScoped;

@Named(value = "movInventarioBean")
@SessionScoped
public class MovInventarioBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private MovInventario movInventario;
    private List<MovInventario> listaMovInventarios;
    private List<MovInventario> movInventariosFiltrados;
    private transient MovInventarioDAO movInventarioDAO;

    // NUEVOS CAMPOS PARA LA INTEGRACIÓN
    private Integer idProductoSeleccionado;
    private List<Producto> listaProductos;
    private transient ProductoDAO productoDAO;
    private transient ServicioInventario servicioInventario;

    private String tipoMovimiento;
    private boolean usarFiltroTrimestre;
    private java.util.Date fechaInicioFiltro;
    private java.util.Date fechaFinFiltro;
    private int añoFiltro;
    private int trimestreFiltro;

    @PostConstruct
    public void init() {
        movInventario = new MovInventario();
        movInventarioDAO = new MovInventarioDAO();
        productoDAO = new ProductoDAO();
        servicioInventario = new ServicioInventario();
        
        listaMovInventarios = movInventarioDAO.listar();
        movInventariosFiltrados = new ArrayList<>();
        
        // Cargar lista de productos
        try {
            listaProductos = productoDAO.listar();
        } catch (SQLException e) {
            listaProductos = new ArrayList<>();
            System.err.println("Error al cargar productos: " + e.getMessage());
        }

        Calendar cal = Calendar.getInstance();
        añoFiltro = cal.get(Calendar.YEAR);
        trimestreFiltro = 1;
    }

    // GETTERS Y SETTERS
    public MovInventario getMovInventario() {
        return movInventario;
    }

    public void setMovInventario(MovInventario movInventario) {
        this.movInventario = movInventario;
    }

    public String getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(String tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
        if (movInventario != null) {
            if ("Entrada".equals(tipoMovimiento)) {
                movInventario.setCantidadSalida(null);
                movInventario.setFechaSalida(null);
                movInventario.setTipoSalida(null);
                movInventario.setCliente(null);
                movInventario.setPrecioVenta(null);
            } else if ("Salida".equals(tipoMovimiento)) {
                movInventario.setCantidadEntrada(null);
                movInventario.setFechaEntrada(null);
                movInventario.setTipoEntrada(null);
                movInventario.setProveedor(null);
                movInventario.setPrecioCompra(null);
                movInventario.setNumeroFactura(null);
            }
        }
    }

    public boolean isTipoEntrada() {
        return "Entrada".equals(tipoMovimiento);
    }

    public boolean isTipoSalida() {
        return "Salida".equals(tipoMovimiento);
    }

    public List<MovInventario> getListaMovInventarios() {
        if (listaMovInventarios == null) {
            listaMovInventarios = getMovInventarioDAO().listar();
        }
        return listaMovInventarios;
    }

    public List<MovInventario> getMovInventariosFiltrados() {
        if (movInventariosFiltrados == null) {
            movInventariosFiltrados = new ArrayList<>();
        }
        return movInventariosFiltrados;
    }

    public void setMovInventariosFiltrados(List<MovInventario> movInventariosFiltrados) {
        this.movInventariosFiltrados = movInventariosFiltrados;
    }

    public boolean isUsarFiltroTrimestre() {
        return usarFiltroTrimestre;
    }

    public void setUsarFiltroTrimestre(boolean usarFiltroTrimestre) {
        this.usarFiltroTrimestre = usarFiltroTrimestre;
    }

    public java.util.Date getFechaInicioFiltro() {
        return fechaInicioFiltro;
    }

    public void setFechaInicioFiltro(java.util.Date fechaInicioFiltro) {
        this.fechaInicioFiltro = fechaInicioFiltro;
    }

    public java.util.Date getFechaFinFiltro() {
        return fechaFinFiltro;
    }

    public void setFechaFinFiltro(java.util.Date fechaFinFiltro) {
        this.fechaFinFiltro = fechaFinFiltro;
    }

    public int getAñoFiltro() {
        return añoFiltro;
    }

    public void setAñoFiltro(int añoFiltro) {
        this.añoFiltro = añoFiltro;
    }

    public int getTrimestreFiltro() {
        return trimestreFiltro;
    }

    public void setTrimestreFiltro(int trimestreFiltro) {
        this.trimestreFiltro = trimestreFiltro;
    }

    // NUEVOS GETTERS Y SETTERS
    public Integer getIdProductoSeleccionado() { 
        return idProductoSeleccionado; 
    }
    
    public void setIdProductoSeleccionado(Integer idProductoSeleccionado) { 
        this.idProductoSeleccionado = idProductoSeleccionado; 
    }
    
    public List<Producto> getListaProductos() {
        if (listaProductos == null) {
            try {
                listaProductos = getProductoDAO().listar();
            } catch (SQLException e) {
                listaProductos = new ArrayList<>();
            }
        }
        return listaProductos;
    }

    // MÉTODOS PARA OBTENER LOS DAOS Y SERVICIOS
    private MovInventarioDAO getMovInventarioDAO() {
        if (movInventarioDAO == null) {
            movInventarioDAO = new MovInventarioDAO();
        }
        return movInventarioDAO;
    }
    
    private ProductoDAO getProductoDAO() {
        if (productoDAO == null) {
            productoDAO = new ProductoDAO();
        }
        return productoDAO;
    }
    
    private ServicioInventario getServicioInventario() {
        if (servicioInventario == null) {
            servicioInventario = new ServicioInventario();
        }
        return servicioInventario;
    }

    // MODIFICAR registrarEntrada PARA USAR EL SERVICIO
    public void registrarEntrada() {
        try {
            System.out.println("=== INICIANDO REGISTRO ENTRADA ===");

            // Validar producto seleccionado
            if (idProductoSeleccionado == null) {
                mostrarError("Seleccione un producto");
                return;
            }

            // Configurar como entrada
            movInventario.setTipoString("Entrada");

            // Limpiar campos de salida
            movInventario.setCantidadSalida(null);
            movInventario.setFechaSalida(null);
            movInventario.setTipoSalida(null);
            movInventario.setCliente(null);
            movInventario.setPrecioVenta(null);

            // Validaciones específicas para entrada
            if (movInventario.getCantidadEntrada() == null || movInventario.getCantidadEntrada() <= 0) {
                mostrarError("La cantidad de entrada debe ser mayor a 0");
                return;
            }
            if (movInventario.getFechaEntrada() == null) {
                mostrarError("Seleccione la fecha de entrada");
                return;
            }
            if (movInventario.getTipoEntrada() == null || movInventario.getTipoEntrada().isEmpty()) {
                mostrarError("Ingrese el tipo de entrada");
                return;
            }
            if (movInventario.getProveedor() == null || movInventario.getProveedor().isEmpty()) {
                mostrarError("Ingrese el proveedor");
                return;
            }
            if (movInventario.getDescripcion() == null || movInventario.getDescripcion().isEmpty()) {
                mostrarError("Ingrese una descripción");
                return;
            }

            System.out.println("Datos a registrar - Producto ID: " + idProductoSeleccionado);
            System.out.println("Cantidad: " + movInventario.getCantidadEntrada());
            System.out.println("Proveedor: " + movInventario.getProveedor());

            // USAR EL SERVICIO EN LUGAR DEL DAO DIRECTO
            getServicioInventario().registrarEntrada(
                idProductoSeleccionado,
                movInventario.getCantidadEntrada(),
                movInventario.getDescripcion(),
                movInventario.getFechaEntrada(),
                movInventario.getTipoEntrada(),
                movInventario.getProveedor(),
                movInventario.getPrecioCompra(),
                movInventario.getNumeroFactura()
            );

            mostrarExito("✅ Entrada registrada y stock actualizado correctamente");
            limpiarFormularioCompleto();
            listaMovInventarios = getMovInventarioDAO().listar();
            
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("❌ Error al registrar entrada: " + e.getMessage());
        }
    }

    // MODIFICAR registrarSalida PARA USAR EL SERVICIO
    public void registrarSalida() {
        try {
            System.out.println("=== INICIANDO REGISTRO SALIDA ===");

            // Validar producto seleccionado
            if (idProductoSeleccionado == null) {
                mostrarError("Seleccione un producto");
                return;
            }

            // Configurar como salida
            movInventario.setTipoString("Salida");

            // Limpiar campos de entrada
            movInventario.setCantidadEntrada(null);
            movInventario.setFechaEntrada(null);
            movInventario.setTipoEntrada(null);
            movInventario.setProveedor(null);
            movInventario.setPrecioCompra(null);
            movInventario.setNumeroFactura(null);

            // Validaciones específicas para salida
            if (movInventario.getCantidadSalida() == null || movInventario.getCantidadSalida() <= 0) {
                mostrarError("La cantidad de salida debe ser mayor a 0");
                return;
            }
            if (movInventario.getFechaSalida() == null) {
                mostrarError("Seleccione la fecha de salida");
                return;
            }
            if (movInventario.getTipoSalida() == null || movInventario.getTipoSalida().isEmpty()) {
                mostrarError("Ingrese el tipo de salida");
                return;
            }
            if (movInventario.getCliente() == null || movInventario.getCliente().isEmpty()) {
                mostrarError("Ingrese el cliente");
                return;
            }
            if (movInventario.getDescripcion() == null || movInventario.getDescripcion().isEmpty()) {
                mostrarError("Ingrese una descripción");
                return;
            }

            System.out.println("Datos a registrar - Producto ID: " + idProductoSeleccionado);
            System.out.println("Cantidad: " + movInventario.getCantidadSalida());
            System.out.println("Cliente: " + movInventario.getCliente());

            // USAR EL SERVICIO EN LUGAR DEL DAO DIRECTO
            getServicioInventario().registrarSalida(
                idProductoSeleccionado,
                movInventario.getCantidadSalida(),
                movInventario.getDescripcion(),
                movInventario.getFechaSalida(),
                movInventario.getTipoSalida(),
                movInventario.getCliente(),
                movInventario.getPrecioVenta()
            );

            mostrarExito("✅ Salida registrada y stock actualizado correctamente");
            limpiarFormularioCompleto();
            listaMovInventarios = getMovInventarioDAO().listar();
            
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("❌ Error al registrar salida: " + e.getMessage());
        }
    }

    public void eliminar(int idMovInventario) {
        try {
            boolean exito = getMovInventarioDAO().eliminarMovimiento(idMovInventario);

            if (exito) {
                mostrarExito("Movimiento eliminado correctamente");
                listaMovInventarios = getMovInventarioDAO().listar();
                if (movInventariosFiltrados != null) {
                    movInventariosFiltrados.removeIf(m -> m.getIdMovInventario() == idMovInventario);
                }
            } else {
                mostrarError("No se pudo eliminar el movimiento");
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al eliminar movimiento: " + e.getMessage());
        }
    }

    public void limpiarFormulario() {
        limpiarFormularioCompleto();
    }

    // LIMPIAR COMPLETAMENTE
    public void limpiarFormularioCompleto() {
        movInventario = new MovInventario();
        tipoMovimiento = null;
        idProductoSeleccionado = null;
    }

    public void toggleFiltroTrimestre() {
        if (usarFiltroTrimestre) {
            fechaInicioFiltro = null;
            fechaFinFiltro = null;
        } else {
            trimestreFiltro = 1;
            añoFiltro = Calendar.getInstance().get(Calendar.YEAR);
        }
    }

    public void aplicarFiltro() {
        try {
            if (usarFiltroTrimestre) {
                aplicarFiltroTrimestre();
                mostrarExito("Filtro por trimestre aplicado: " + getDescripcionFiltro());
            } else {
                if (fechaInicioFiltro != null && fechaFinFiltro != null) {
                    aplicarFiltroFechas();
                    mostrarExito("Filtro por fechas aplicado: " + getDescripcionFiltro());
                } else {
                    mostrarError("Seleccione ambas fechas para filtrar");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al aplicar filtro: " + e.getMessage());
        }
    }

    private void aplicarFiltroTrimestre() {
        try {
            movInventariosFiltrados = getMovInventarioDAO().filtrarPorTrimestre(añoFiltro, trimestreFiltro);
            System.out.println("Filtro trimestre aplicado. Resultados: " + movInventariosFiltrados.size());
        } catch (Exception e) {
            System.err.println("Error en filtro trimestre: " + e.getMessage());
            movInventariosFiltrados = new ArrayList<>();
        }
    }

    private void aplicarFiltroFechas() {
        if (fechaInicioFiltro != null && fechaFinFiltro != null) {
            Calendar calFin = Calendar.getInstance();
            calFin.setTime(fechaFinFiltro);
            calFin.set(Calendar.HOUR_OF_DAY, 23);
            calFin.set(Calendar.MINUTE, 59);
            calFin.set(Calendar.SECOND, 59);

            movInventariosFiltrados = getMovInventarioDAO().filtrarPorFechas(fechaInicioFiltro, calFin.getTime());
        } else {
            movInventariosFiltrados = new ArrayList<>();
            mostrarError("Seleccione ambas fechas para filtrar");
        }
    }

    public void limpiarFiltro() {
        movInventariosFiltrados = new ArrayList<>();
        fechaInicioFiltro = null;
        fechaFinFiltro = null;
        añoFiltro = Calendar.getInstance().get(Calendar.YEAR);
        trimestreFiltro = 1;
        usarFiltroTrimestre = false;
        mostrarExito("Filtros limpiados correctamente");
    }

    // MÉTODOS PARA ESTADÍSTICAS
    public int getTotalEntradas() {
        return getMovInventarioDAO().getTotalEntradas();
    }

    public int getTotalSalidas() {
        return getMovInventarioDAO().getTotalSalidas();
    }

    // MÉTODOS PARA LISTAS
    public List<Integer> getAniosDisponibles() {
        List<Integer> anios = new ArrayList<>();
        int añoActual = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = añoActual; i >= añoActual - 5; i--) {
            anios.add(i);
        }
        return anios;
    }

    public Map<String, Integer> getTrimestres() {
        Map<String, Integer> trimestres = new LinkedHashMap<>();
        trimestres.put("Primer Trimestre (Ene-Mar)", 1);
        trimestres.put("Segundo Trimestre (Abr-Jun)", 2);
        trimestres.put("Tercer Trimestre (Jul-Sep)", 3);
        trimestres.put("Cuarto Trimestre (Oct-Dic)", 4);
        return trimestres;
    }

    public String getDescripcionFiltro() {
        if (usarFiltroTrimestre) {
            String[] nombresTrimestres = {"", "Primer Trimestre (Ene-Mar)", "Segundo Trimestre (Abr-Jun)",
                "Tercer Trimestre (Jul-Sep)", "Cuarto Trimestre (Oct-Dic)"};
            return "Filtrado por: " + nombresTrimestres[trimestreFiltro] + " del " + añoFiltro;
        } else if (fechaInicioFiltro != null && fechaFinFiltro != null) {
            return "Filtrado por fechas: "
                    + new java.text.SimpleDateFormat("dd/MM/yyyy").format(fechaInicioFiltro) + " - "
                    + new java.text.SimpleDateFormat("dd/MM/yyyy").format(fechaFinFiltro);
        } else {
            return "Mostrando todos los movimientos";
        }
    }

    public List<String> getTiposMovimiento() {
        return Arrays.asList("Entrada", "Salida");
    }

    private void mostrarExito(String mensaje) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", mensaje));
    }

    private void mostrarError(String mensaje) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", mensaje));
    }
}