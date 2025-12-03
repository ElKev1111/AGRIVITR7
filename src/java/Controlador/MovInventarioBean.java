package Controlador;

import DAO.MovInventarioDAO;
import DAO.ProductoDAO;
import Modelo.MovInventario;
import Modelo.Producto;
import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;
import javax.faces.bean.ViewScoped;
import org.primefaces.model.file.UploadedFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.faces.context.ExternalContext;
import org.primefaces.event.FileUploadEvent;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.faces.bean.ManagedProperty;
import Modelo.EnumRoles;


@Named(value = "movInventarioBean")
@SessionScoped
public class MovInventarioBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private UploadedFile imagenProducto;
    private MovInventario movInventario;
    private List<MovInventario> listaMovInventarios;
    private List<MovInventario> movInventariosFiltrados;
    private transient MovInventarioDAO movInventarioDAO;
    private MovInventario movSeleccionado;
    private Producto productoDetalle;
    private UploadedFile archivoFactura;

    // NUEVOS CAMPOS PARA LA INTEGRACIÓN
    private Integer idProductoSeleccionado;
    private List<Producto> listaProductos;
    private transient ProductoDAO productoDAO;

    private String tipoMovimiento;

    // Filtro por fechas / trimestre
    private boolean usarFiltroTrimestre;
    private java.util.Date fechaInicioFiltro;
    private java.util.Date fechaFinFiltro;
    private int añoFiltro;
    private int trimestreFiltro;

    // Filtros adicionales (panel superior)
    private String filtroTipo;                // Entrada / Salida
    private Integer filtroProductoId;         // id de producto
    private String filtroTipoEspecifico;      // COMPRA, VENTA, DEVOLUCION, CADUCADO
    private String filtroProveedorCliente;    // proveedor o cliente
    // Fecha de vencimiento del producto seleccionado (solo para mostrarla en el modal de salida)
    private Date fechaVencimientoProducto;

    public Date getFechaVencimientoProducto() {
        return fechaVencimientoProducto;
    }

    public void setFechaVencimientoProducto(Date fechaVencimientoProducto) {
        this.fechaVencimientoProducto = fechaVencimientoProducto;
    }

    @PostConstruct
    public void init() {
        movInventario = new MovInventario();
        listaMovInventarios = new ArrayList<>();
        listaProductos = new ArrayList<>();
        movInventariosFiltrados = new ArrayList<>();

        cargarProductos();      // llena listaProductos
        cargarMovimientos();    // llena listaMovInventarios

        Calendar cal = Calendar.getInstance();
        añoFiltro = cal.get(Calendar.YEAR);
        trimestreFiltro = 1;
    }

    public UploadedFile getImagenProducto() {
        return imagenProducto;
    }

    public void prepararVerDetalle(MovInventario mov) {
        this.movSeleccionado = mov;

        // Cargar datos del producto para mostrar el nombre, etc.
        try {
            if (mov != null && mov.getIdProducto() != null) {
                this.productoDetalle = getProductoDAO().buscar(mov.getIdProducto());
            } else {
                this.productoDetalle = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.productoDetalle = null;
        }
    }

    // GETTERS Y SETTERS
    public MovInventario getMovInventario() {
        return movInventario;
    }

    public UploadedFile getArchivoFactura() {
        return archivoFactura;
    }

    public void setArchivoFactura(UploadedFile archivoFactura) {
        this.archivoFactura = archivoFactura;
    }

    public void setMovInventario(MovInventario movInventario) {
        this.movInventario = movInventario;
    }

    public void setImagenProducto(UploadedFile imagenProducto) {
        this.imagenProducto = imagenProducto;
    }

    public MovInventario getMovSeleccionado() {
        return movSeleccionado;
    }

    public void setMovSeleccionado(MovInventario movSeleccionado) {
        this.movSeleccionado = movSeleccionado;
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

    public String getRutaImagenMovimientoSeleccionado() {
        if (movSeleccionado == null || movSeleccionado.getImagenProducto() == null) {
            return null;
        }
        return FacesContext.getCurrentInstance().getExternalContext()
                .getRequestContextPath() + movSeleccionado.getImagenProducto();
    }

// REGISTRAR ENTRADA 
    public void registrarEntrada() {
        try {
            if (idProductoSeleccionado == null) {
                mostrarError("Seleccione un producto");
                return;
            }

            if (movInventario.getCantidadEntrada() == null
                    || movInventario.getCantidadEntrada() <= 0) {
                mostrarError("La cantidad de entrada debe ser mayor a 0");
                return;
            }

            if (movInventario.getFechaEntrada() == null) {
                mostrarError("Seleccione la fecha de entrada");
                return;
            }

            if (movInventario.getTipoEntrada() == null
                    || movInventario.getTipoEntrada().isEmpty()) {
                mostrarError("Ingrese el tipo de entrada");
                return;
            }

            if (movInventario.getProveedor() == null
                    || movInventario.getProveedor().isEmpty()) {
                mostrarError("Ingrese el proveedor");
                return;
            }

            if (movInventario.getDescripcion() == null
                    || movInventario.getDescripcion().isEmpty()) {
                mostrarError("Ingrese una descripción");
                return;
            }

            if (movInventario.getPrecioCompra() != null
                    && movInventario.getPrecioCompra() > 0) {
                double compra = movInventario.getPrecioCompra();
                double venta = compra * 1.15;
                movInventario.setPrecioVenta(venta);
            }

            Producto productoDB = getProductoDAO().buscar(idProductoSeleccionado);
            if (productoDB == null) {
                mostrarError("No se encontró el producto seleccionado");
                return;
            }

            int stockAnterior = productoDB.getStock();
            int stockNuevo = stockAnterior + movInventario.getCantidadEntrada();

            movInventario.setTipoString("Entrada");
            movInventario.setCantidadSalida(null);
            movInventario.setFechaSalida(null);
            movInventario.setTipoSalida(null);
            movInventario.setCliente(null);

            movInventario.setIdProducto(idProductoSeleccionado);
            movInventario.setStockAnterior(stockAnterior);
            movInventario.setStockNuevo(stockNuevo);

            // ==============================
            // USUARIO QUE REGISTRA (FINAL)
            // ==============================
            movInventario.setUsuarioRegistro(obtenerUsuarioRegistro());

            getMovInventarioDAO().registrarMovimiento(movInventario);
            getProductoDAO().actualizarStock(idProductoSeleccionado, stockNuevo);

            mostrarExito("Entrada registrada y stock actualizado correctamente");

            movInventario = new MovInventario();
            archivoFactura = null;
            imagenProducto = null;
            cargarMovimientos();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al registrar entrada: " + e.getMessage());
        }
    }

    public void recalcularPrecioVenta() {
        if (movInventario.getPrecioCompra() != null) {
            double compra = movInventario.getPrecioCompra();
            double venta = compra * 1.15;
            movInventario.setPrecioVenta(venta);
        }
    }

    private void guardarArchivoFactura() throws IOException {
        if (archivoFactura == null || archivoFactura.getSize() == 0) {
            return;
        }

        String rutaBase = "C:/facturas";

        String nombreArchivo
                = System.currentTimeMillis() + "_" + archivoFactura.getFileName();

        Path destino = Paths.get(rutaBase, nombreArchivo);

        Files.copy(archivoFactura.getInputStream(),
                destino,
                StandardCopyOption.REPLACE_EXISTING);
    }

    @ManagedProperty("#{sesionUsuarioBean}")
    private SesionUsuarioBean sesionUsuarioBean;

    public SesionUsuarioBean getSesionUsuarioBean() {
        return sesionUsuarioBean;
    }

    public void setSesionUsuarioBean(SesionUsuarioBean sesionUsuarioBean) {
        this.sesionUsuarioBean = sesionUsuarioBean;
    }

    public void onProductoCaducadoChange() {
        if (idProductoSeleccionado == null) {
            movInventario.setProveedor(null);
            fechaVencimientoProducto = null;
            return;
        }

        try {
            Producto prod = productoDAO.buscar(idProductoSeleccionado);
            if (prod != null) {
                // Fecha de vencimiento solo para mostrarla (ya la teníamos)
                this.fechaVencimientoProducto = prod.getFechaVencimiento();

                // Proveedor del producto -> se usa en modal y en la tabla
                movInventario.setProveedor(prod.getNombreProveedor());

                // NO tocar fechaSalida, el usuario la elige
                // movInventario.setFechaSalida(null); (esto solo si quieres limpiar al cambiar producto)
            }
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarError("Error al cargar datos del producto: " + e.getMessage());
        }
    }

    public void subirFactura(FileUploadEvent event) {
        this.archivoFactura = event.getFile();
        if (archivoFactura == null) {
            return;
        }

        try {
            String nombreOriginal = archivoFactura.getFileName();
            String nombreLimpio = nombreOriginal.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
            String nombreFinal = System.currentTimeMillis() + "_" + nombreLimpio;

            String rutaRelativa = "/resources/uploads/facturas/" + nombreFinal;

            String rutaAbsoluta = FacesContext.getCurrentInstance()
                    .getExternalContext()
                    .getRealPath(rutaRelativa);

            File destino = new File(rutaAbsoluta);
            destino.getParentFile().mkdirs();

            try (InputStream in = archivoFactura.getInputStream(); FileOutputStream out = new FileOutputStream(destino)) {

                byte[] buffer = new byte[1024];
                int bytes;
                while ((bytes = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytes);
                }
            }

            movInventario.setArchivoFactura(rutaRelativa);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al subir la factura: " + e.getMessage());
        }
    }

    public Producto getProductoDetalle() {
        return productoDetalle;
    }

    public void setProductoDetalle(Producto productoDetalle) {
        this.productoDetalle = productoDetalle;
    }

    private void cargarMovimientos() {
        try {
            listaMovInventarios = getMovInventarioDAO().listar();
        } catch (Exception e) {
            listaMovInventarios = new ArrayList<>();
            e.printStackTrace();
            mostrarError("Error al cargar los movimientos de inventario: " + e.getMessage());
        }
    }

    private void cargarProductos() {
        try {
            listaProductos = getProductoDAO().listar();
        } catch (Exception e) {
            e.printStackTrace();
            listaProductos = new ArrayList<>();
        }
    }

    public void onProductoChange() {
        if (idProductoSeleccionado == null) {
            return;
        }

        try {
            Producto prod = productoDAO.buscar(idProductoSeleccionado);

            if (prod != null) {
                movInventario.setProveedor(prod.getNombreProveedor());
                double compra = prod.getPrecioProducto();
                movInventario.setPrecioCompra(compra);
                movInventario.setPrecioVenta(compra * 1.15);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            mostrarError("Error al cargar datos del producto: " + e.getMessage());
        }
    }

    public void validarCantidad(FacesContext context, UIComponent component, Object value)
            throws ValidatorException {

        if (value == null) {
            throw new ValidatorException(
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Cantidad requerida", "Debe ingresar la cantidad"));
        }

        int cantidad = ((Number) value).intValue();
        String tipo = movInventario != null ? movInventario.getTipoEntrada() : null;

        if ("COMPRA".equalsIgnoreCase(tipo)) {
            if (cantidad < 10) {
                throw new ValidatorException(
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Cantidad inválida",
                                "Para compras la cantidad mínima es 10"));
            }
        } else if ("DEVOLUCION".equalsIgnoreCase(tipo)) {
            if (cantidad < 1) {
                throw new ValidatorException(
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Cantidad inválida",
                                "Para devoluciones la cantidad mínima es 1"));
            }
        }
    }

    private String guardarArchivo(UploadedFile archivo, String carpetaRelativa) throws IOException {
        if (archivo == null || archivo.getSize() == 0) {
            return null;
        }

        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext external = context.getExternalContext();
        String rutaRealCarpeta = external.getRealPath(carpetaRelativa);

        Path dir = Paths.get(rutaRealCarpeta);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        String nombreOriginal = Paths.get(archivo.getFileName()).getFileName().toString();
        String nombreFinal = System.currentTimeMillis() + "_" + nombreOriginal;

        Path destino = dir.resolve(nombreFinal);

        Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

        return carpetaRelativa + "/" + nombreFinal;
    }

    public boolean isDevolucion() {
        return movInventario != null
                && "DEVOLUCION".equalsIgnoreCase(movInventario.getTipoEntrada());
    }

    public boolean isCompra() {
        return movInventario != null
                && "COMPRA".equalsIgnoreCase(movInventario.getTipoEntrada());
    }

    public boolean isTipoEntrada() {
        return "Entrada".equals(tipoMovimiento);
    }

    public boolean isTipoSalida() {
        return "Salida".equals(tipoMovimiento);
    }

    public List<MovInventario> getListaMovInventarios() {
        if (listaMovInventarios == null) {
            cargarMovimientos();
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

    public void prepararNuevaEntrada() {
        cargarProductos();
        movInventario = new MovInventario();
        idProductoSeleccionado = null;

        movInventario.setTipoString("Entrada");
        movInventario.setTipoEntrada("");
        movInventario.setFechaEntrada(new Date());
        movInventario.setFechaSalida(null);
        movInventario.setPrecioCompra(null);
        movInventario.setPrecioVenta(null);
    }

    public void prepararNuevaSalida() {
        movInventario = new MovInventario();
        idProductoSeleccionado = null;

        movInventario.setTipoString("Salida");
        movInventario.setTipoSalida("CADUCADO");

        movInventario.setCantidadSalida(null);
        movInventario.setFechaSalida(null);
        movInventario.setDescripcion(null);
    }

    // Filtros por fecha / trimestre
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

    // Filtros adicionales (panel superior)
    public String getFiltroTipo() {
        return filtroTipo;
    }

    public void setFiltroTipo(String filtroTipo) {
        this.filtroTipo = filtroTipo;
    }

    public Integer getFiltroProductoId() {
        return filtroProductoId;
    }

    public void setFiltroProductoId(Integer filtroProductoId) {
        this.filtroProductoId = filtroProductoId;
    }

    public String getFiltroTipoEspecifico() {
        return filtroTipoEspecifico;
    }

    public void setFiltroTipoEspecifico(String filtroTipoEspecifico) {
        this.filtroTipoEspecifico = filtroTipoEspecifico;
    }

    public String getFiltroProveedorCliente() {
        return filtroProveedorCliente;
    }

    public void setFiltroProveedorCliente(String filtroProveedorCliente) {
        this.filtroProveedorCliente = filtroProveedorCliente;
    }

    // NUEVOS GETTERS Y SETTERS DE PRODUCTOS
    public Integer getIdProductoSeleccionado() {
        return idProductoSeleccionado;
    }

    public void setIdProductoSeleccionado(Integer idProductoSeleccionado) {
        this.idProductoSeleccionado = idProductoSeleccionado;
    }

    public List<Producto> getListaProductos() {
        // Siempre recargar productos desde la BD para tener la lista al día
        cargarProductos();
        return listaProductos;
    }

    public void setListaProductos(List<Producto> listaProductos) {
        this.listaProductos = listaProductos;
    }

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

// REGISTRAR SALIDA (CADUCADO)
    public void registrarSalida() {
        try {

            if (idProductoSeleccionado == null) {
                mostrarError("Seleccione un producto");
                return;
            }

            movInventario.setTipoString("Salida");
            movInventario.setTipoSalida("CADUCADO");
            movInventario.setTipoEntrada(null);
            movInventario.setCliente(null);

            if (movInventario.getCantidadSalida() == null || movInventario.getCantidadSalida() <= 0) {
                mostrarError("La cantidad de salida debe ser mayor a 0");
                return;
            }

            if (movInventario.getFechaSalida() == null) {
                mostrarError("Seleccione la fecha de salida");
                return;
            }

            Producto productoDB = getProductoDAO().buscar(idProductoSeleccionado);
            if (productoDB == null) {
                mostrarError("No se encontró el producto seleccionado");
                return;
            }

            movInventario.setProveedor(productoDB.getNombreProveedor());
            movInventario.setDescripcion("Producto vencido");

            if (movInventario.getCantidadSalida() > productoDB.getStock()) {
                mostrarError("No hay suficiente stock para completar la salida");
                return;
            }

            int stockAnterior = productoDB.getStock();
            int stockNuevo = stockAnterior - movInventario.getCantidadSalida();

            movInventario.setIdProducto(idProductoSeleccionado);
            movInventario.setStockAnterior(stockAnterior);
            movInventario.setStockNuevo(stockNuevo);

            // ==============================
            // USUARIO QUE REGISTRA (FINAL)
            // ==============================
            movInventario.setUsuarioRegistro(obtenerUsuarioRegistro());

            getMovInventarioDAO().registrarMovimiento(movInventario);
            getProductoDAO().actualizarStock(idProductoSeleccionado, stockNuevo);

            mostrarExito("Salida registrada y stock actualizada correctamente");

            movInventario = new MovInventario();
            cargarMovimientos();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al registrar salida: " + e.getMessage());
        }
    }

    public void eliminar(int idMovInventario) {
        try {
            boolean exito = getMovInventarioDAO().eliminarMovimiento(idMovInventario);

            if (exito) {
                mostrarExito("Movimiento eliminado correctamente");
                cargarMovimientos();

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

    // ---- APLICAR FILTROS (panel superior) ----
    public void aplicarFiltro() {
        try {
            if (usarFiltroTrimestre) {
                aplicarFiltroTrimestre();
                mostrarExito("Filtro por trimestre aplicado: " + getDescripcionFiltro());
            } else if (fechaInicioFiltro != null && fechaFinFiltro != null) {
                aplicarFiltroFechas();
                mostrarExito("Filtro por fechas aplicado: " + getDescripcionFiltro());
            } else {
                // Sin fechas ni trimestre: filtramos toda la lista en memoria
                movInventariosFiltrados = new ArrayList<>(listaMovInventarios);
                aplicarFiltrosAdicionales();
                mostrarExito("Filtro aplicado");
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al aplicar filtro: " + e.getMessage());
        }
    }

    // Aplica filtros por tipo, producto, tipo específico y proveedor/cliente
    private void aplicarFiltrosAdicionales() {
        if (movInventariosFiltrados == null) {
            movInventariosFiltrados = new ArrayList<>();
            return;
        }

        List<MovInventario> resultado = new ArrayList<>();

        for (MovInventario m : movInventariosFiltrados) {

            // Tipo Entrada/Salida
            if (filtroTipo != null && !filtroTipo.isEmpty()) {
                if (m.getTipoString() == null || !filtroTipo.equals(m.getTipoString())) {
                    continue;
                }
            }

            // Producto
            if (filtroProductoId != null) {
                if (m.getIdProducto() == null || !filtroProductoId.equals(m.getIdProducto())) {
                    continue;
                }
            }

            // Tipo específico
            String tipoEsp = "Entrada".equals(m.getTipoString()) ? m.getTipoEntrada() : m.getTipoSalida();
            if (filtroTipoEspecifico != null && !filtroTipoEspecifico.isEmpty()) {
                if (tipoEsp == null || !filtroTipoEspecifico.equalsIgnoreCase(tipoEsp)) {
                    continue;
                }
            }

            // Proveedor / Cliente
            String provCli = "Entrada".equals(m.getTipoString()) ? m.getProveedor() : m.getCliente();
            if (filtroProveedorCliente != null && !filtroProveedorCliente.isEmpty()) {
                if (provCli == null || !filtroProveedorCliente.equalsIgnoreCase(provCli.trim())) {
                    continue;
                }
            }

            resultado.add(m);
        }

        movInventariosFiltrados = resultado;
    }

    private void aplicarFiltroTrimestre() {
        try {
            movInventariosFiltrados = getMovInventarioDAO().filtrarPorTrimestre(añoFiltro, trimestreFiltro);
            aplicarFiltrosAdicionales();
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
            aplicarFiltrosAdicionales();
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

        filtroTipo = null;
        filtroProductoId = null;
        filtroTipoEspecifico = null;
        filtroProveedorCliente = null;

        mostrarExito("Filtros limpiados correctamente");
    }

    // MÉTODOS PARA ESTADÍSTICAS
    public int getTotalEntradas() {
        return getMovInventarioDAO().getTotalEntradas();
    }

    public int getTotalSalidas() {
        return getMovInventarioDAO().getTotalSalidas();
    }

    // ===== LISTAS ÚNICAS PARA LOS FILTROS EN EL PANEL =====
    public List<Integer> getProductoIdsUnicos() {
        Set<Integer> set = new LinkedHashSet<>();
        if (listaMovInventarios != null) {
            for (MovInventario m : listaMovInventarios) {
                if (m.getIdProducto() != null) {
                    set.add(m.getIdProducto());
                }
            }
        }
        List<Integer> lista = new ArrayList<>(set);
        Collections.sort(lista);
        return lista;
    }

    public List<String> getProveedoresClientesUnicos() {
        Set<String> set = new LinkedHashSet<>();
        if (listaMovInventarios != null) {
            for (MovInventario m : listaMovInventarios) {
                String valor = m.getProveedor(); // usamos siempre proveedor (entradas y salidas caducadas)

                if (valor != null && !valor.trim().isEmpty()) {
                    set.add(valor.trim());
                }
            }
        }
        List<String> lista = new ArrayList<>(set);
        Collections.sort(lista, String.CASE_INSENSITIVE_ORDER);
        return lista;
    }

    // MÉTODOS PARA LISTAS EN EL PANEL
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

    private String obtenerUsuarioRegistro() {

        try {
            UsuarioBean usuarioBean = (UsuarioBean) FacesContext.getCurrentInstance()
                    .getExternalContext()
                    .getSessionMap()
                    .get("usuarioBean");

            if (usuarioBean != null && usuarioBean.isAutenticado() && usuarioBean.getUsuario() != null) {

                String nombre = usuarioBean.getUsuario().getNombre();   // tiene getNombre()
                EnumRoles rol = usuarioBean.getUsuario().getRol();      // EnumRoles

                if (nombre != null && !nombre.trim().isEmpty()) {
                    if (rol != null) {
                        return nombre + " (" + rol.name() + ")";
                    } else {
                        return nombre;
                    }
                }
            }

        } catch (Exception e) {
            // ignorar, se devolverá el default
        }

        return "Usuario desconocido";
    }

}
