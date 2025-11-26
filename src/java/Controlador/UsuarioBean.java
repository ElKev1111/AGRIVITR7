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

    
@ManagedBean
@SessionScoped
public class UsuarioBean implements Serializable {
    private static final long serialVersionUID = 1L;
    

    private Usuario usuario = new Usuario();
    private UsuarioDAO usuarioDAO = new UsuarioDAO();
    
    private boolean autenticado = false;

    public boolean isAutenticado() {
        return usuario != null && usuario.getId() > 0;
    }

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
    public String autenticar() {
        String destino = null;

        try (Connection con = Conexion.conectar()) {
            String sql = "SELECT * FROM usuario WHERE correo = ? AND password = ? ";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, usuario.getCorreo());

            String password = CifradoAES.encriptar(usuario.getPassword()); // aca se encripta el valor ingrasdado como password en el login
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                this.usuario = new Usuario();
                usuario.setId(rs.getInt("id"));
                usuario.setNombre(rs.getString("nombre"));
                usuario.setCorreo(rs.getString("correo"));
                usuario.setEstado(rs.getString("estado"));

                String rolDb = rs.getString("rol");
                EnumRoles rol = EnumRoles.valueOf(rolDb.trim().toUpperCase(Locale.ROOT));
                usuario.setRol(rol);

                if (usuario.getEstado() != null && !"ACTIVO".equalsIgnoreCase(usuario.getEstado())) {
                    this.autenticado = false;
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso", "Tu usuario está inactivo. Contacta con el administrador."));
                    return null;
                }

                this.autenticado = true;

                FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("user", usuario.getNombre());

                if (rol == EnumRoles.ADMINISTRADOR || rol == EnumRoles.EMPLEADO) {
                    destino = "HomeAdmin1?faces-redirect=true";
                } else {
                    destino = "dashboardCliente?faces-redirect=true";
                }

            } else {
                this.autenticado = false;
                
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso", "Id de Usuario y/o Contraseña no válidos"));
            }

        } catch (SQLException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_FATAL, "Error", "Error en Conexión a Base de Datos"));
        }

        return destino;
    }

    public String logout() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();

            // PASO CRUCIAL: Invalidar la sesión HTTP
            context.getExternalContext().invalidateSession();

            this.usuario = new Usuario();
            this.autenticado = false;

            // Redirigir al login (usando faces-redirect=true para limpieza)
            return "dashboardPresentacion?faces-redirect=true";

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
            usuario.setEstado("ACTIVO");

            String passEncriptada = CifradoAES.encriptar(usuario.getPassword());
            usuario.setPassword(passEncriptada);

            usuario.setRol(EnumRoles.CLIENTE);
            usuarioDAO.agregar(usuario);

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Éxito", "Usuario registrado correctamente."));
            this.autenticado = false;

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
    public void verificarSesionCliente() {
    try {
        // Si no está autenticado, redirigir al login
        if (!isAutenticado() || usuario.getRol() != EnumRoles.CLIENTE) {
            FacesContext.getCurrentInstance().getExternalContext()
                        .redirect("login.xhtml");
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}

    public void cambiarEstado(Usuario u) {
        try {
            String nuevoEstado = "ACTIVO".equalsIgnoreCase(u.getEstado()) ? "INACTIVO" : "ACTIVO";
            if (usuarioDAO.actualizarEstado(u.getId(), nuevoEstado)) {
                u.setEstado(nuevoEstado);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Estado actualizado", "El usuario ahora está " + nuevoEstado));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudo cambiar el estado del usuario"));
        }
    }
    
    
}
