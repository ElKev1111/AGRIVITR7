package Controlador;

import DAO.UsuarioDAO;
import Modelo.CifradoAES;
import Modelo.Usuario;
import Modelo.EnumRoles;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@ManagedBean
@SessionScoped
public class UsuarioBean implements Serializable {

    private static final long serialVersionUID = 1L;

    // Usuario LOGUEADO
    private Usuario usuario = new Usuario();

    // Acceso a BD
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    // Flag de ayuda
    private boolean autenticado = false;

    // =========================
    //  GETTERS / SETTERS
    // =========================
    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public boolean isAutenticado() {
        // Consideramos autenticado si tiene id y el flag está en true
        return usuario != null && usuario.getId() > 0 && autenticado;
    }

    // Texto listo para mostrar / guardar como “UsuarioRegistro”
    public String getEtiquetaUsuarioActual() {
        if (usuario == null || usuario.getId() <= 0) {
            return "";
        }

        String nombre = usuario.getNombre();
        EnumRoles rol = usuario.getRol();

        String rolTexto = "";
        if (rol != null) {
            // admin -> "Admin", cliente -> "Cliente", etc.
            String raw = rol.name().toLowerCase(Locale.ROOT);
            rolTexto = raw.substring(0, 1).toUpperCase(Locale.ROOT) + raw.substring(1);
        }

        if (nombre == null) {
            nombre = "";
        }

        if (rolTexto.isEmpty()) {
            return nombre;
        }
        return nombre + " (" + rolTexto + ")";
    }

    public List<Usuario> getListaUsuarios() {
        try {
            return usuarioDAO.listar();
        } catch (SQLException e) {
            System.out.println("Error al listar usuarios: " + e.getMessage());
            return null;
        }
    }

    public String getEtiquetaUsuario() {

        if (usuario == null) {
            return "Usuario desconocido";
        }

        // nombre del usuario
        String nombre = usuario.getNombre();

        // nombre del rol desde EnumRoles
        String rol = (usuario.getRol() != null) ? usuario.getRol().name() : "";

        if (nombre == null || nombre.trim().isEmpty()) {
            return "Usuario desconocido";
        }

        if (rol == null || rol.trim().isEmpty()) {
            return nombre;
        }

        return nombre + " (" + rol + ")";
    }

    // =========================
    //  AUTENTICACIÓN
    // =========================
    public String autenticar() {
        String destino = null;

        try (Connection con = Conexion.conectar()) {

            String sql = "SELECT * FROM usuario WHERE correo = ? AND password = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, usuario.getCorreo());

            // password ingresada en el login
            String password = CifradoAES.encriptar(usuario.getPassword());
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // Rellenar objeto usuario logueado
                usuario = new Usuario();
                usuario.setId(rs.getInt("id"));
                usuario.setNombre(rs.getString("nombre"));
                usuario.setCorreo(rs.getString("correo"));
                usuario.setEstado(rs.getString("estado"));

                String rolDb = rs.getString("rol");
                EnumRoles rol = EnumRoles.valueOf(rolDb.trim().toUpperCase(Locale.ROOT));
                usuario.setRol(rol);

                // Validar estado
                if (usuario.getEstado() != null
                        && !"ACTIVO".equalsIgnoreCase(usuario.getEstado())) {

                    autenticado = false;
                    FacesContext.getCurrentInstance().addMessage(
                            null,
                            new FacesMessage(
                                    FacesMessage.SEVERITY_WARN,
                                    "Aviso",
                                    "Tu usuario está inactivo. Contacta con el administrador."
                            )
                    );
                    return null;
                }

                autenticado = true;

                // Guardamos algo en sesión si quieres
                FacesContext.getCurrentInstance()
                        .getExternalContext()
                        .getSessionMap()
                        .put("user", usuario.getNombre());

                // Redirección según rol
                if (rol == EnumRoles.ADMINISTRADOR || rol == EnumRoles.EMPLEADO) {
                    destino = "HomeAdmin1?faces-redirect=true";
                } else {
                    destino = "dashboardCliente?faces-redirect=true";
                }

            } else {
                autenticado = false;
                FacesContext.getCurrentInstance().addMessage(
                        null,
                        new FacesMessage(
                                FacesMessage.SEVERITY_WARN,
                                "Aviso",
                                "Correo y/o contraseña no válidos"
                        )
                );
            }

        } catch (SQLException e) {
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_FATAL,
                            "Error",
                            "Error en Conexión a Base de Datos"
                    )
            );
        }

        return destino;
    }

    public String logout() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();

            // Invalidar sesión HTTP
            context.getExternalContext().invalidateSession();

            // Limpiar datos en bean
            usuario = new Usuario();
            autenticado = false;

            // Volver a la presentación
            return "dashboardPresentacion?faces-redirect=true";

        } catch (Exception e) {
            e.printStackTrace();
            return "login?faces-redirect=true";
        }
    }

    // =========================
    //  REGISTRO DE NUEVO CLIENTE
    // =========================
    public void agregar() throws IOException {
        try {
            usuario.setFecha_creacion(LocalDateTime.now());
            usuario.setFecha_actualizacion(LocalDateTime.now());
            usuario.setEstado("ACTIVO");

            String passEncriptada = CifradoAES.encriptar(usuario.getPassword());
            usuario.setPassword(passEncriptada);

            // Todo el que se registra aquí es CLIENTE
            usuario.setRol(EnumRoles.CLIENTE);

            usuarioDAO.agregar(usuario);

            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_INFO,
                            "Éxito",
                            "Usuario registrado correctamente."
                    )
            );

            autenticado = false;

            // Limpiar formulario
            usuario = new Usuario();

            // Volver al login
            FacesContext.getCurrentInstance().getExternalContext()
                    .redirect("login.xhtml");

        } catch (SQLException e) {
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            "Error",
                            "Usuario no registrado."
                    )
            );
        }
    }

    // =========================
    //  GUARDAS DE PÁGINA
    // =========================
    public void verificarSesionCliente() {
        try {
            if (!isAutenticado() || usuario.getRol() != EnumRoles.CLIENTE) {
                FacesContext.getCurrentInstance().getExternalContext()
                        .redirect("login.xhtml");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================
    //  ADMIN: CAMBIAR ESTADO
    // =========================
    public void cambiarEstado(Usuario u) {
        try {
            String nuevoEstado = "ACTIVO".equalsIgnoreCase(u.getEstado())
                    ? "INACTIVO"
                    : "ACTIVO";

            if (usuarioDAO.actualizarEstado(u.getId(), nuevoEstado)) {
                u.setEstado(nuevoEstado);
                FacesContext.getCurrentInstance().addMessage(
                        null,
                        new FacesMessage(
                                FacesMessage.SEVERITY_INFO,
                                "Estado actualizado",
                                "El usuario ahora está " + nuevoEstado
                        )
                );
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            "Error",
                            "No se pudo cambiar el estado del usuario"
                    )
            );
        }
    }
}
