package DAO;

import Modelo.Producto;
import Controlador.Conexion;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProductoDAO implements Serializable {

    private static final long serialversionUID = 1;

    private transient PreparedStatement ps;
    private transient ResultSet rs;

    public List<Producto> listar() throws SQLException {
        List<Producto> listaProductos = new ArrayList<>();
        String sql = "SELECT idProducto, nombreProducto, idProveedor, nombreProveedor, precioProducto, descripcion, tipo, fechaIngreso, fechaVencimiento, stock FROM producto";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Producto p = mapearProductoDesdeResultSet(rs);
                listaProductos.add(p);
            }
        } catch (SQLException e) {
            System.out.println("Error al listar los productos: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        return listaProductos;
    }

   
    public List<Producto> listarPorProveedor(int idProveedor) throws SQLException {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT * FROM producto WHERE idProveedor = ?";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idProveedor);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Producto p = new Producto();
                    p.setIdProducto(rs.getInt("idProducto"));
                    p.setNombreProducto(rs.getString("nombreProducto"));
                    p.setIdProveedor(rs.getInt("idProveedor"));
                    p.setNombreProveedor(rs.getString("nombreProveedor"));
                    p.setPrecioProducto(rs.getFloat("precioProducto"));
                    p.setDescripcion(rs.getString("descripcion"));
                    p.setFechaVencimiento(rs.getTimestamp("fechaVencimiento"));
                    p.setTipo(rs.getString("tipo"));
                    p.setFechaIngreso(rs.getTimestamp("fechaIngreso"));
                    p.setStock(rs.getInt("stock"));
                    lista.add(p);
                }
            }
        }
        return lista;
    }

    public void agregar(Producto p) throws SQLException {
        String sql = "INSERT INTO producto(nombreProducto, idProveedor, nombreProveedor, precioProducto, descripcion, tipo, fechaIngreso, fechaVencimiento, stock) "
                + "VALUES(?,?,?,?,?,?,?,?,?)";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, p.getNombreProducto());
            ps.setInt(2, p.getIdProveedor());
            ps.setString(3, p.getNombreProveedor());
            ps.setFloat(4, p.getPrecioProducto());
            ps.setString(5, p.getDescripcion());
            ps.setString(6, p.getTipo());

            // fechaIngreso (nullable)
            Date fechaIngreso = p.getFechaIngreso();
            if (fechaIngreso != null) {
                ps.setTimestamp(7, new Timestamp(fechaIngreso.getTime()));
            } else {
                ps.setNull(7, java.sql.Types.TIMESTAMP);
            }

            // fechaVencimiento (nullable)
            Date fechaVencimiento = p.getFechaVencimiento();
            if (fechaVencimiento != null) {
                ps.setTimestamp(8, new Timestamp(fechaVencimiento.getTime()));
            } else {
                ps.setNull(8, java.sql.Types.TIMESTAMP);
            }

            ps.setInt(9, p.getStock());

            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al registrar producto: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Actualiza un producto existente. Actualiza los campos principales y
     * fechas/stock.
     */
    public void actualizar(Producto p) throws SQLException {
        String sql = "UPDATE producto SET nombreProducto = ?, nombreProveedor = ?, idProveedor = ?, precioProducto = ?, descripcion = ?, tipo = ?, fechaIngreso = ?, fechaVencimiento = ?, stock = ? WHERE idProducto = ?";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, p.getNombreProducto());
            ps.setString(2, p.getNombreProveedor());
            ps.setInt(3, p.getIdProveedor());
            ps.setFloat(4, p.getPrecioProducto());
            ps.setString(5, p.getDescripcion());
            ps.setString(6, p.getTipo());

            // fechaIngreso (nullable)
            Date fechaIngreso = p.getFechaIngreso();
            if (fechaIngreso != null) {
                ps.setTimestamp(7, new Timestamp(fechaIngreso.getTime()));
            } else {
                ps.setNull(7, java.sql.Types.TIMESTAMP);
            }

            // fechaVencimiento (nullable)
            Date fechaVencimiento = p.getFechaVencimiento();
            if (fechaVencimiento != null) {
                ps.setTimestamp(8, new Timestamp(fechaVencimiento.getTime()));
            } else {
                ps.setNull(8, java.sql.Types.TIMESTAMP);
            }

            ps.setInt(9, p.getStock());
            ps.setInt(10, p.getIdProducto());

            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al actualizar producto: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Elimina un producto por id.
     */
    public void eliminar(Producto p) throws SQLException {
        String sql = "DELETE FROM producto WHERE idProducto = ?";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, p.getIdProducto());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al eliminar producto: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public boolean actualizarStock(int idProducto, int nuevoStock) throws SQLException {
        String sql = "UPDATE producto SET stock = ? WHERE idProducto = ?";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, nuevoStock);
            ps.setInt(2, idProducto);
            return ps.executeUpdate() > 0;
        }
    }

//    public Producto buscar(int id) throws SQLException {
//        Producto p = null;
//        String sql = "SELECT idProducto, nombreProducto, idProveedor, nombreProveedor, precioProducto, descripcion, tipo, fechaIngreso, fechaVencimiento, stock FROM producto WHERE idProducto = ?";
//
//        try (Connection con = Conexion.conectar();
//             PreparedStatement ps = con.prepareStatement(sql)) {
//
//            ps.setInt(1, id);
//
//            try (ResultSet rs = ps.executeQuery()) {
//                if (rs.next()) {
//                    p = mapearProductoDesdeResultSet(rs);
//                }
//            }
//        } catch (SQLException e) {
//            System.out.println("Error al buscar el producto: " + e.getMessage());
//            e.printStackTrace();
//            throw e;
//        }
//
//        return p;
//    }
    public Producto buscar(int idProducto) throws SQLException {
        String sql = "SELECT * FROM producto WHERE idProducto = ?";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Producto p = new Producto();
                    p.setIdProducto(rs.getInt("idProducto"));
                    p.setNombreProducto(rs.getString("nombreProducto"));
                    p.setIdProveedor(rs.getInt("idProveedor"));
                    p.setNombreProveedor(rs.getString("nombreProveedor"));
                    p.setPrecioProducto(rs.getFloat("precioProducto"));
                    p.setDescripcion(rs.getString("descripcion"));
                    p.setFechaVencimiento(rs.getTimestamp("fechaVencimiento"));
                    p.setTipo(rs.getString("tipo"));
                    p.setFechaIngreso(rs.getTimestamp("fechaIngreso"));
                    p.setStock(rs.getInt("stock"));
                    return p;
                }
            }
        }
        return null;
    }

    private Producto mapearProductoDesdeResultSet(ResultSet rs) throws SQLException {
        Producto p = new Producto();

        p.setIdProducto(rs.getInt("idProducto"));
        p.setNombreProducto(rs.getString("nombreProducto"));
        p.setIdProveedor(rs.getInt("idProveedor"));
        p.setNombreProveedor(rs.getString("nombreProveedor"));
        p.setPrecioProducto(rs.getFloat("precioProducto"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setTipo(rs.getString("tipo"));

        Timestamp tsIngreso = rs.getTimestamp("fechaIngreso");
        if (tsIngreso != null) {
            p.setFechaIngreso(new Date(tsIngreso.getTime()));
        } else {
            p.setFechaIngreso(null);
        }

        Timestamp tsVenc = null;
        try {
            tsVenc = rs.getTimestamp("fechaVencimiento");
        } catch (SQLException ex) {
            // Si la columna no existe por alguna razón, la ignoramos (pero idealmente la tabla debe tenerla).
            tsVenc = null;
        }

        if (tsVenc != null) {
            p.setFechaVencimiento(new Date(tsVenc.getTime()));
        } else {
            p.setFechaVencimiento(null);
        }

        try {
            p.setStock(rs.getInt("stock"));
        } catch (SQLException ex) {
            // Si la columna stock no existe, evitar excepción; puedes ajustar según tu esquema.
            p.setStock(0);
        }

        return p;
    }
    public boolean tieneMovimientosInventario(int idProducto) throws SQLException {
    String sql = "SELECT 1 FROM movimientos_inventario WHERE idProducto = ? LIMIT 1";
    try (Connection con = Conexion.conectar();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setInt(1, idProducto);
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next();
        }
    }
}

}
