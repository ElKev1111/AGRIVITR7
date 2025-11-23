package DAO;

import Modelo.Usuario;
import Controlador.Conexion;
import Modelo.EnumRoles;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UsuarioDAO implements Serializable {
     private static final long serialVersionUID = 1L;

    PreparedStatement ps;
    ResultSet rs;

    public List<Usuario> listar() throws SQLException {
        List<Usuario> listaUsuarios = new ArrayList<>();
        try {
            String sql = "SELECT * FROM usuario";

            ps = Conexion.conectar().prepareStatement(sql);

            rs = ps.executeQuery();

            while (rs.next()) {
                if("Cliente".equalsIgnoreCase(rs.getString("rol"))){
                Usuario u = new Usuario();
                u.setId(rs.getInt("id"));
                u.setRol(EnumRoles.CLIENTE);
                u.setNombre(rs.getString("nombre"));
                u.setCorreo(rs.getString("correo"));
                u.setCelular(rs.getString("celular"));
                u.setFecha_actualizacion(rs.getTimestamp("fecha_actualizacion").toLocalDateTime());
                u.setFecha_creacion(rs.getTimestamp("fecha_creacion").toLocalDateTime());
                u.setDireccion(rs.getString("direccion"));
                u.setPassword(rs.getString("password"));

                listaUsuarios.add(u);
                }
            }

        } catch (SQLException e) {

        }
        return listaUsuarios;
    }

    public void agregar(Usuario u) throws SQLException {
        String sql = "INSERT INTO usuario (rol, nombre, correo, celular, fecha_actualizacion, fecha_creacion, direccion, password) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = Conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, u.getRol().name().toLowerCase());
            ps.setString(2, u.getNombre());
            ps.setString(3, u.getCorreo());
            ps.setString(4, u.getCelular());
            ps.setTimestamp(5, Timestamp.valueOf(u.getFecha_actualizacion()));
            ps.setTimestamp(6, Timestamp.valueOf(u.getFecha_creacion()));     
            ps.setString(7, u.getDireccion());
            ps.setString(8, u.getPassword());

            ps.executeUpdate();

            System.out.println("Usuario agregado con éxito");
            
        } catch (SQLException e) {
            System.out.println("Error al registrar usuario: " + e.getMessage());
            throw e;
        }
    }
    public void actualizar(Usuario u) {
        try {
            String sql = "UPDATE usuario SET rol=?, nombre=?, correo=?,celular=?, direccion =? WHERE id=?";
            ps = Conexion.conectar().prepareStatement(sql);

            ps.setString(1, u.getRol().name().toLowerCase());
            ps.setString(2, u.getNombre());
            ps.setString(3, u.getCorreo());
            ps.setString(4, u.getCelular());
            ps.setString(5, u.getDireccion());
            ps.setInt(6, u.getId());
             
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println(" Error al actualizar usuario: " + e.getMessage());
        }
    }


}
