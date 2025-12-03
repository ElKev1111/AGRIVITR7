package Controlador;

import DAO.ProductoDAO;
import DAO.ProveedorDAO;
import Modelo.Producto;
import Modelo.Proveedor;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

// Subida de archivos
import org.primefaces.model.file.UploadedFile;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@ManagedBean
@ViewScoped
public class ProductoBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private Producto producto = new Producto();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();

    private List<Producto> listaProductos;     // cache en memoria
    private List<Proveedor> listaProveedores;
    private Integer idProveedorSeleccionado;

    // Archivo de imagen para crear / editar
    private UploadedFile archivoFotoProducto;

    // ================== GETTERS / SETTERS ==================

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public UploadedFile getArchivoFotoProducto() {
        return archivoFotoProducto;
    }

    public void setArchivoFotoProducto(UploadedFile archivoFotoProducto) {
        this.archivoFotoProducto = archivoFotoProducto;
    }

    public Integer getIdProveedorSeleccionado() {
        return idProveedorSeleccionado;
    }

    public void setIdProveedorSeleccionado(Integer idProveedorSeleccionado) {
        this.idProveedorSeleccionado = idProveedorSeleccionado;
    }

    // ================== INIT ==================

    @PostConstruct
    public void init() {
        listaProductos = new ArrayList<>();
        try {
            List<Producto> desdeBD = productoDAO.listar();
            if (desdeBD != null) {
                listaProductos = desdeBD;
            }
        } catch (SQLException e) {
            System.out.println("Error al listar los productos: " + e.getMessage());
            // dejamos listaProductos como lista vacía
        }
    }

    // ================== FECHA DE VENCIMIENTO ==================

    // Fecha mínima de vencimiento: hoy + 1 mes (a la medianoche)
    public Date getMinFechaVencimiento() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        cal.add(Calendar.MONTH, 1);
        return cal.getTime();
    }

    public void validarFechaVencimiento(FacesContext context,
                                        UIComponent component,
                                        Object value) throws ValidatorException {
        if (value == null) {
            return;
        }

        Date fecha = (Date) value;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.MONTH, 1);
        Date minimo = cal.getTime();

        if (fecha.before(minimo)) {
            throw new ValidatorException(
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            "Fecha de vencimiento inválida",
                            "La fecha de vencimiento debe ser al menos un mes después de hoy."
                    )
            );
        }
    }

    // ================== LISTAS ==================

    // Nunca retorna null
    public List<Producto> getListaProductos() {
        System.out.println(">>> EJECUTANDO getter getListaProductos()");
        try {
            List<Producto> lista = productoDAO.listar();
            return (lista != null) ? lista : new ArrayList<>();
        } catch (Exception e) {
            System.out.println("Error al listar los productos: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // stock actual para otras vistas
    public int getStockActual(int idProducto) {
        try {
            Producto p = productoDAO.buscar(idProducto);
            return (p != null) ? p.getStock() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // Nunca null
    public List<Proveedor> getListaProveedores() {
        if (listaProveedores == null) {
            listaProveedores = new ArrayList<>();
            try {
                List<Proveedor> desdeBD = proveedorDAO.listar();
                if (desdeBD != null) {
                    listaProveedores = desdeBD;
                }
            } catch (SQLException e) {
                mostrarError("Error al cargar proveedores: " + e.getMessage());
            }
        }
        return listaProveedores;
    }

    // Productos por proveedor
    public List<Producto> getProductosPorProveedor(int idProveedor) {
        try {
            List<Producto> lista = productoDAO.listarPorProveedor(idProveedor);
            return (lista != null) ? lista : new ArrayList<>();
        } catch (SQLException e) {
            mostrarError("Error al listar productos por proveedor");
            return new ArrayList<>();
        }
    }

    // ================== CRUD ==================

    public String agregar() {
        try {
            if (producto.getNombreProducto() == null || producto.getNombreProducto().isEmpty()) {
                mostrarError("El nombre del producto es requerido");
                return null;
            }
            if (idProveedorSeleccionado == null) {
                mostrarError("Debe seleccionar un proveedor");
                return null;
            }

            // Fecha de ingreso
            producto.setFechaIngreso(new Date());

            // Cargar proveedor
            Proveedor proveedor = proveedorDAO.buscar(idProveedorSeleccionado);
            if (proveedor == null) {
                mostrarError("No se encontró el proveedor seleccionado");
                return null;
            }

            producto.setIdProveedor(proveedor.getIdProveedor());
            producto.setNombreProveedor(proveedor.getNombreProveedor());

            if (producto.getStock() < 0) {
                producto.setStock(0);
            }

            // Guardar imagen si se subió una
            if (archivoFotoProducto != null
                    && archivoFotoProducto.getFileName() != null
                    && !archivoFotoProducto.getFileName().isEmpty()) {

                String nombreArchivo = System.currentTimeMillis() + "_"
                        + Paths.get(archivoFotoProducto.getFileName())
                               .getFileName().toString();

                String rutaProductos = FacesContext.getCurrentInstance()
                        .getExternalContext()
                        .getRealPath("/resources/images/products");

                File directorio = new File(rutaProductos);
                if (!directorio.exists()) {
                    directorio.mkdirs();
                }

                Path destino = Paths.get(directorio.getAbsolutePath(), nombreArchivo);

                try (InputStream input = archivoFotoProducto.getInputStream()) {
                    Files.copy(input, destino, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    mostrarError("Error al guardar la imagen del producto: " + ex.getMessage());
                    return null;
                }

                producto.setImagen(nombreArchivo);
            }

            // Guardar en BD
            productoDAO.agregar(producto);

            // Refrescar cache
            try {
                List<Producto> desdeBD = productoDAO.listar();
                listaProductos = (desdeBD != null) ? desdeBD : new ArrayList<>();
            } catch (SQLException ex) {
                listaProductos = new ArrayList<>();
            }

            // Limpiar formulario
            producto = new Producto();
            idProveedorSeleccionado = null;
            archivoFotoProducto = null;

            mostrarExito("Producto creado correctamente");
            return "HomeAdmin3?faces-redirect=true";

        } catch (SQLException e) {
            System.out.println("Error al insertar el producto: " + e.getMessage());
            mostrarError("Error al crear producto: " + e.getMessage());
            return null;
        }
    }

    public void editar(Producto p) {
        this.producto = p;
        // no limpiamos archivoFotoProducto aquí para poder cargar uno nuevo en el diálogo
        archivoFotoProducto = null;
    }

    public void actualizar() {
        try {
            // Si se subió una nueva imagen durante la edición
            if (archivoFotoProducto != null
                    && archivoFotoProducto.getFileName() != null
                    && !archivoFotoProducto.getFileName().isEmpty()) {

                String nombreArchivo = System.currentTimeMillis() + "_"
                        + Paths.get(archivoFotoProducto.getFileName())
                               .getFileName().toString();

                String rutaProductos = FacesContext.getCurrentInstance()
                        .getExternalContext()
                        .getRealPath("/resources/images/products");

                System.out.println("Ruta física productos (actualizar) = " + rutaProductos);

                if (rutaProductos == null) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                    "Error",
                                    "No se pudo resolver la ruta física para las imágenes de productos."));
                    return;
                }

                File directorio = new File(rutaProductos);
                if (!directorio.exists()) {
                    directorio.mkdirs();
                }

                Path destino = Paths.get(directorio.getAbsolutePath(), nombreArchivo);

                try (InputStream input = archivoFotoProducto.getInputStream()) {
                    Files.copy(input, destino, StandardCopyOption.REPLACE_EXISTING);
                }

                // Actualizamos el nombre de la imagen en el producto
                producto.setImagen(nombreArchivo);
            }

            // Actualizar en BD
            productoDAO.actualizar(producto);

            // Refrescar lista
            try {
                List<Producto> desdeBD = productoDAO.listar();
                listaProductos = (desdeBD != null) ? desdeBD : new ArrayList<>();
            } catch (SQLException ex) {
                listaProductos = new ArrayList<>();
            }

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Éxito", "Producto actualizado correctamente"));

            producto = new Producto();
            archivoFotoProducto = null;

        } catch (Exception e) {
            System.out.println("Error al actualizar producto: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", "No se pudo actualizar el producto: " + e.getMessage()));
        }
    }

    public void eliminar(Producto p) {
        try {
            // 1. Verificar si tiene movimientos de inventario
            if (productoDAO.tieneMovimientosInventario(p.getIdProducto())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                                "Aviso",
                                "No se puede eliminar el producto '" + p.getNombreProducto()
                                        + "' porque tiene movimientos de inventario registrados."));
                return; // Salimos sin eliminar
            }

            // 2. Si no tiene movimientos, sí se elimina
            productoDAO.eliminar(p);

            // refrescar lista
            try {
                List<Producto> desdeBD = productoDAO.listar();
                listaProductos = (desdeBD != null) ? desdeBD : new ArrayList<>();
            } catch (SQLException ex) {
                listaProductos = new ArrayList<>();
            }

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Éxito", "Producto eliminado correctamente"));

        } catch (SQLException e) {
            System.out.println("Error al eliminar producto: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", "No se pudo eliminar el producto: " + e.getMessage()));
        }
    }

    // ================== MENSAJES ==================

    private void mostrarExito(String mensaje) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", mensaje));
    }

    private void mostrarError(String mensaje) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", mensaje));
    }
}
