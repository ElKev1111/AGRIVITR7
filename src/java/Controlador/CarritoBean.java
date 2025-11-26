package Controlador;

import Modelo.Producto;
import Modelo.CarritoItem;
import DAO.ProductoDAO;
import DAO.VentasDAO;
import DAO.MovInventarioDAO;
import Modelo.EnumRoles;
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
import javax.servlet.http.HttpServletResponse;
import javax.faces.context.ExternalContext;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@ManagedBean
@SessionScoped
public class CarritoBean implements Serializable {

    private List<CarritoItem> items = new ArrayList<>();
    private Integer cantidadTemporal = 1;
    private Producto productoTemporal;
    private CarritoItem itemEdicion;

    private List<Ventas> ventasUltimoPago = new ArrayList<>();

    public List<Ventas> getVentasUltimoPago() {
        return ventasUltimoPago;
    }

    private transient ProductoDAO productoDAO = new ProductoDAO();
    private transient VentasDAO ventasDAO = new VentasDAO();
    private transient MovInventarioDAO movInventarioDAO = new MovInventarioDAO();
    private boolean simulacionPublicaInicializada;

    // === DATOS PARA SIMULACIÓN DE PAGO EN LÍNEA ===
    private String nombreTitular;
    private String numeroTarjeta;
    private String fechaExpiracion;
    private String cvv;
    private String tipoTarjeta;

    private String codigoOperacionUltimoPago;
    private LocalDateTime fechaUltimoPago;

    public void prepararSimulacionPublica() {
        // Restablece el carrito cuando se entra por primera vez al dashboard público
        // para que la simulación termine al recargar o cerrar la vista.
        if (!usuarioAutenticado() && !FacesContext.getCurrentInstance().isPostback() && !simulacionPublicaInicializada) {
            items.clear();
            simulacionPublicaInicializada = true;
        }
    }

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
        System.out.println("📝 Preparando edición: " + item.getProducto().getNombreProducto()
                + ", Cantidad actual: " + item.getCantidad());
    }

    public void actualizarCantidad() {
    if (itemEdicion == null || cantidadTemporal <= 0) {
        return;
    }

    int cantidadAnterior = itemEdicion.getCantidad();

    // 1) Actualizamos el objeto de la lista explícitamente
    for (CarritoItem it : items) {
        if (it.getProducto().getIdProducto() == itemEdicion.getProducto().getIdProducto()) {
            it.setCantidad(cantidadTemporal);
            break;
        }
    }

    // 2) Por si acaso, también actualizamos la referencia almacenada
    itemEdicion.setCantidad(cantidadTemporal);

    System.out.println("✅ Cantidad actualizada: "
            + itemEdicion.getProducto().getNombreProducto()
            + " de " + cantidadAnterior + " a " + cantidadTemporal);

    FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Cantidad Actualizada",
                    itemEdicion.getProducto().getNombreProducto()
                    + " actualizado a " + cantidadTemporal + " unidades."));

    // Limpieza
    itemEdicion = null;
}

    public void procederAlPago() {
    // Validación básica de carrito
    if (isVacio()) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Carrito vacío",
                        "No puedes proceder al pago con el carrito vacío."));
        return;
    }

    // Validación de sesión
    if (!usuarioAutenticado()) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Inicia sesión",
                        "Debes iniciar sesión como cliente para completar el pago."));
        redirigirALogin();
        return;
    }

    // Validar datos de tarjeta (lado servidor)
    if (!validarDatosTarjeta()) {
        return; // si hay errores, no continúa
    }

    // Si JSF marcó errores de validación (por required, regex, etc.)
    if (FacesContext.getCurrentInstance().isValidationFailed()) {
        return;
    }

    try {
        procesarPago();  // registra ventas, movimientos, etc.

        // Código de operación simulado
        this.codigoOperacionUltimoPago = "AGV-" + System.currentTimeMillis();
        this.fechaUltimoPago = LocalDateTime.now();

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Pago exitoso",
                        "Tu pago se procesó correctamente. Código de operación: " + codigoOperacionUltimoPago));

        limpiarDatosTarjeta();

    } catch (Exception e) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Error en pago",
                        "No se pudo procesar el pago: " + e.getMessage()));
    }
}
    private void limpiarDatosTarjeta() {
    nombreTitular = null;
    numeroTarjeta = null;
    fechaExpiracion = null;
    cvv = null;
    tipoTarjeta = null;
}

    private UsuarioBean obtenerUsuarioBean() {
        return FacesContext.getCurrentInstance().getApplication()
                .evaluateExpressionGet(FacesContext.getCurrentInstance(), "#{usuarioBean}", UsuarioBean.class);
    }

    private boolean usuarioAutenticado() {
        UsuarioBean usuarioBean = obtenerUsuarioBean();
        return usuarioBean != null
                && usuarioBean.isAutenticado()
                && usuarioBean.getUsuario().getRol() == EnumRoles.CLIENTE;
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
            // Guardamos todas las ventas de este pago para el comprobante
            ventasUltimoPago = new ArrayList<>();
            double totalGeneral = 0;

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

                ventasUltimoPago.add(venta);
                totalGeneral += venta.getTotalPagar();
            }

            items.clear();

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Pago registrado",
                            "Se simuló el pago, se actualizó el inventario y se generó el comprobante."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error en pago",
                            "No se pudo procesar el pago: " + e.getMessage()));
        }
    }

    public void descargarComprobantePdf() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        try {
            if (ventasUltimoPago == null || ventasUltimoPago.isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Sin comprobante",
                                "No hay un pago reciente para generar el comprobante."));
                return;
            }

            UsuarioBean usuarioBean = obtenerUsuarioBean();

            ExternalContext ec = ctx.getExternalContext();
            HttpServletResponse response = (HttpServletResponse) ec.getResponse();

            response.reset();
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"comprobante_pago.pdf\"");

            Document document = new Document();
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            document.add(new Paragraph("AgriviApp - Comprobante de Pago"));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Cliente: " + usuarioBean.getUsuario().getNombre()));
            document.add(new Paragraph("Correo: " + usuarioBean.getUsuario().getCorreo()));
            document.add(new Paragraph("Fecha: " + LocalDateTime.now().toString()));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4);
            table.addCell("Producto");
            table.addCell("Cantidad");
            table.addCell("Precio unitario");
            table.addCell("Subtotal");

            double total = 0;
            for (Ventas v : ventasUltimoPago) {
                table.addCell(v.getProducto().getNombreProducto());
                table.addCell(String.valueOf(v.getCantidad()));
                table.addCell(String.format("%.2f", v.getProducto().getPrecioVenta()));
                table.addCell(String.format("%.2f", v.getTotalPagar()));
                total += v.getTotalPagar();
            }

            document.add(table);
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Total pagado: $" + String.format("%.2f", total)));

            document.close();
            ctx.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
                            "No se pudo generar el comprobante PDF: " + e.getMessage()));
        }
    }
    private boolean validarDatosTarjeta() {
    boolean valido = true;
    FacesContext ctx = FacesContext.getCurrentInstance();

    if (nombreTitular == null || nombreTitular.trim().isEmpty()) {
        ctx.addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_WARN,
                "Dato requerido",
                "El nombre del titular es obligatorio."
        ));
        valido = false;
    }

    if (numeroTarjeta == null) {
        ctx.addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_WARN,
                "Dato requerido",
                "El número de tarjeta es obligatorio."
        ));
        valido = false;
    } else {
        String soloDigitos = numeroTarjeta.replaceAll("\\s+", "");
        if (!soloDigitos.matches("\\d{16}")) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Número de tarjeta inválido",
                    "La tarjeta debe tener 16 dígitos numéricos."
            ));
            valido = false;
        }
    }

    if (fechaExpiracion == null || !fechaExpiracion.matches("(0[1-9]|1[0-2])/(\\d{2})")) {
        ctx.addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_WARN,
                "Fecha inválida",
                "La fecha de expiración debe tener el formato MM/AA."
        ));
        valido = false;
    }

    if (cvv == null || !cvv.matches("\\d{3}")) {
        ctx.addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_WARN,
                "CVV inválido",
                "El CVV debe tener 3 dígitos numéricos."
        ));
        valido = false;
    }

    if (tipoTarjeta == null || tipoTarjeta.trim().isEmpty()) {
        ctx.addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_WARN,
                "Dato requerido",
                "Debes seleccionar el tipo de tarjeta."
        ));
        valido = false;
    }

    return valido;
}

    public String getCodigoOperacionUltimoPago() {
        return codigoOperacionUltimoPago;
    }

    public LocalDateTime getFechaUltimoPago() {
        return fechaUltimoPago;
    }
// Getters y Setters
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

    public List<CarritoItem> getItems() {
        return items;
    }

    public Integer getCantidadTemporal() {
    return cantidadTemporal;
}

public void setCantidadTemporal(Integer cantidadTemporal) {
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

    public String getNombreTitular() {
        return nombreTitular;
    }

    public void setNombreTitular(String nombreTitular) {
        this.nombreTitular = nombreTitular;
    }

    public String getNumeroTarjeta() {
        return numeroTarjeta;
    }

    public void setNumeroTarjeta(String numeroTarjeta) {
        this.numeroTarjeta = numeroTarjeta;
    }

    public String getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(String fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public String getTipoTarjeta() {
        return tipoTarjeta;
    }

    public void setTipoTarjeta(String tipoTarjeta) {
        this.tipoTarjeta = tipoTarjeta;
    }

}
