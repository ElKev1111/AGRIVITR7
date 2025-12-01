package DAO;

import Modelo.Ventas;
import Modelo.Usuario;
import Controlador.Conexion;
import Modelo.Producto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VentasDAO {

    PreparedStatement ps;
    ResultSet rs;

    // LISTAR TODAS LAS VENTAS (para HomeAdmin4)
    public List<Ventas> listar() {
        List<Ventas> listaVentas = new ArrayList<>();

        String sql =
                "SELECT v.idVenta, v.fechaVenta, v.idProducto, v.idUsuario, v.cantidad, v.totalPagar, " +
                "       p.nombreProducto, p.precioProducto, " +
                "       u.nombre AS nombreUsuario, u.correo AS correoUsuario " +
                "FROM ventas v " +
                "JOIN producto p ON p.idProducto = v.idProducto " +
                "JOIN usuario u ON u.id = v.idUsuario " +   // 👈 ajusta 'usuario' y 'id' si tus nombres son distintos
                "ORDER BY v.fechaVenta DESC";

        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Ventas venta = new Ventas();
                Producto producto = new Producto();
                Usuario usuario = new Usuario();

                venta.setIdVenta(rs.getInt("idVenta"));

                Timestamp ts = rs.getTimestamp("fechaVenta");
                if (ts != null) {
                    venta.setFechaVenta(ts.toLocalDateTime());
                }

                // ===== Producto =====
                producto.setIdProducto(rs.getInt("idProducto"));
                producto.setNombreProducto(rs.getString("nombreProducto"));
                producto.setPrecioProducto(rs.getFloat("precioProducto"));
                venta.setProducto(producto);

                // ===== Usuario (cliente) =====
                usuario.setId(rs.getInt("idUsuario"));
                usuario.setNombre(rs.getString("nombreUsuario"));
                usuario.setCorreo(rs.getString("correoUsuario"));
                venta.setUsuario(usuario);

                // ===== Datos de la venta =====
                venta.setCantidad(rs.getInt("cantidad"));
                venta.setTotalPagar(rs.getFloat("totalPagar"));

                listaVentas.add(venta);
            }

        } catch (SQLException e) {
            System.out.println("Error al listar ventas: " + e.getMessage());
        }

        return listaVentas;
    }

    // REGISTRAR VENTA (como lo tenías)
    public boolean registrar(Ventas venta) {
        String sql = "INSERT INTO ventas (fechaVenta, idProducto, idUsuario, cantidad, totalPagar) VALUES (?,?,?,?,?)";

        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, venta.getFechaVenta() != null
                    ? Timestamp.valueOf(venta.getFechaVenta())
                    : new Timestamp(new Date().getTime()));
            ps.setInt(2, venta.getProducto().getIdProducto());
            ps.setInt(3, venta.getUsuario().getId());
            ps.setInt(4, venta.getCantidad());
            ps.setFloat(5, venta.getTotalPagar());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al registrar venta: " + e.getMessage());
            return false;
        }
    }

    // LISTAR VENTAS POR USUARIO (historial del cliente)
    public List<Ventas> listarPorUsuario(int idUsuario) {
        List<Ventas> listaVentas = new ArrayList<>();

        String sql =
                "SELECT v.idVenta, v.fechaVenta, v.idProducto, v.idUsuario, v.cantidad, v.totalPagar, " +
                "       p.nombreProducto, p.precioProducto " +
                "FROM ventas v " +
                "JOIN producto p ON p.idProducto = v.idProducto " +
                "WHERE v.idUsuario = ? " +
                "ORDER BY v.fechaVenta DESC";

        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ventas venta = new Ventas();
                    Producto producto = new Producto();
                    Usuario usuario = new Usuario();

                    venta.setIdVenta(rs.getInt("idVenta"));

                    Timestamp ts = rs.getTimestamp("fechaVenta");
                    if (ts != null) {
                        venta.setFechaVenta(ts.toLocalDateTime());
                    }

                    producto.setIdProducto(rs.getInt("idProducto"));
                    producto.setNombreProducto(rs.getString("nombreProducto"));
                    producto.setPrecioProducto(rs.getFloat("precioProducto"));
                    venta.setProducto(producto);

                    usuario.setId(rs.getInt("idUsuario"));
                    venta.setUsuario(usuario);

                    venta.setCantidad(rs.getInt("cantidad"));
                    venta.setTotalPagar(rs.getFloat("totalPagar"));

                    listaVentas.add(venta);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al listar ventas del usuario: " + e.getMessage());
        }

        return listaVentas;
    }
}
