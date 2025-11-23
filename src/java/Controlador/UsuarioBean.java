    package Controlador;

import DAO.UsuarioDAO;
import Modelo.CifradoAES;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import Modelo.Usuario;
import Modelo.EnumRoles;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ViewScoped;

    
@ManagedBean
//@ApplicationScoped
@ViewScoped
public class UsuarioBean implements Serializable {
    private static final long serialVersionUID = 1L;
    

    private Usuario usuario = new Usuario();
    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    // Getter & Setter
    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public List<Usuario> getListaUsuarios() {
        try {
            return usuarioDAO.listar();

        } catch (SQLException e) {
            System.out.println("Erro al listar usuarios");
            return null;
        }
    }

    public String editar(Usuario u) {
        this.usuario = u;
        return "editarUsuario?faces-redirect=true";

    }
    
    public String actualizar(Usuario u){
        try{
            u.setFecha_actualizacion(LocalDateTime.now());
            usuarioDAO.actualizar(u);
            
            FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, 
                "Éxito", "Habitaciónactualizada correctamente"));
    } catch (Exception e) {
        FacesContext.getCurrentInstance().addMessage(null,
          new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                  "Error", "No se pudo actualizar la habitación"));       
    }
        return "HomeAdmin1?faces-redirect=true";
    }
    

    // Método de autenticación
    public void autenticar() throws SQLException, IOException {
        try {
            Connection con = Conexion.conectar();

            String sql = "SELECT * FROM usuario WHERE correo = ? AND password = ? ";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, usuario.getCorreo());

            String password = CifradoAES.encriptar(usuario.getPassword()); // aca se encripta el valor ingrasdado como password en el login
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("user", rs.getString("nombre"));

                String rolDb = rs.getString("rol");
                EnumRoles rol = EnumRoles.valueOf(rolDb.trim().toUpperCase(Locale.ROOT));

                if (rol == EnumRoles.ADMINISTRADOR || rol== EnumRoles.EMPLEADO) {
                    FacesContext.getCurrentInstance().getExternalContext().redirect("HomeAdmin1.xhtml");
                } else {
                    FacesContext.getCurrentInstance().getExternalContext().redirect("dashboardCliente.xhtml");
                }

            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso", "Id de Usuario y/o Contraseña no válidos"));
            }

        } catch (SQLException | IOException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_FATAL, "Error", "Error en Conexión a Base de Datos"));

        }
    }

    public String logout() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();

            // PASO CRUCIAL: Invalidar la sesión HTTP
            context.getExternalContext().invalidateSession();

            // Redirigir al login (usando faces-redirect=true para limpieza)
            return "login?faces-redirect=true";

        } catch (Exception e) {
            // Manejo de errores
            e.printStackTrace();
            return "login?faces-redirect=true";
        }
    }

    public void agregar() throws IOException {
        try {
            usuario.setFecha_creacion(LocalDateTime.now());
            usuario.setFecha_actualizacion(LocalDateTime.now());

            String passEncriptada = CifradoAES.encriptar(usuario.getPassword());
            usuario.setPassword(passEncriptada);

            usuario.setRol(EnumRoles.CLIENTE);
            usuarioDAO.agregar(usuario);

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Éxito", "Usuario registrado correctamente."));

            // Limpiar formulario
            usuario = new Usuario();
            FacesContext.getCurrentInstance().getExternalContext()
                    .redirect("login.xhtml");

        } catch (SQLException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Error", "Usuario no registrado ."));
        }
    }
    
    
}
