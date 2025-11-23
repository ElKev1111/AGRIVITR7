package DAO;

import Modelo.Ventas;
import Modelo.Usuario;
import Controlador.Conexion;
import Modelo.EnumRoles;
import Modelo.Producto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date; // Necesario para obtener la fecha actual del sistema
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VentasDAO {

    PreparedStatement ps;
    ResultSet rs;

  public List<Ventas> listar() {
    List<Ventas> listaVentas = new ArrayList<>();
    String sql = "SELECT idVenta, fechaVenta, idProducto, idUsuario, cantidad, totalPagar FROM ventas";

    try {
        ps = Conexion.conectar().prepareStatement(sql);
        rs = ps.executeQuery();

        while (rs.next()) {
            Ventas venta = new Ventas();
            Producto producto = new Producto();
            Usuario usuario = new Usuario();

            venta.setIdVenta(rs.getInt("idVenta"));

            Timestamp ts = rs.getTimestamp("fechaVenta");
            if(ts != null){
                venta.setFechaVenta(ts.toLocalDateTime());
            }

            producto.setIdProducto(rs.getInt("idProducto"));
            venta.setProducto(producto);

            usuario.setId(rs.getInt("idUsuario"));
            venta.setUsuario(usuario);

            venta.setCantidad(rs.getInt("cantidad"));
            venta.setTotalPagar(rs.getFloat("totalPagar"));

            listaVentas.add(venta);
        }

    } catch (SQLException e) {
        System.out.println("Error al listar ventas: " + e.getMessage());
    }

    return listaVentas;
}


}
