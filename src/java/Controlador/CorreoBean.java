package Controlador;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import Modelo.Usuario;

@ManagedBean
@ViewScoped
public class CorreoBean {
    private String asunto;
    private String contmensaje;
    List<String> dest;
    List<Usuario> listaUsr;
    
    public void listarUsuarios(){
        listaUsr = new ArrayList<>();
        
        try {
            String sql = "SELECT * FROM usuario";
            PreparedStatement ps =  Conexion.conectar().prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            while(rs.next()){
                Usuario usr = new Usuario();
                usr.setNombre(rs.getString("nombre"));
                usr.setCorreo(rs.getString("correo"));
                
                listaUsr.add(usr);
            }
        } catch (SQLException e) {
        }
    }
    
    public void enviarCorreo(){
        final String user = "kalvinalonzo@gmail.com";
        final String pass = "fjre wsvh lsbn exyw";
        
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        
        //Autenticación de Usuario
        Session sesion = Session.getInstance(props, new Authenticator(){           
            @Override
            protected PasswordAuthentication getPasswordAuthentication(){
                return new PasswordAuthentication(user, pass);
            } 
        });
        
        //Preparar Mensaje
        try {
            Message mensaje = new MimeMessage(sesion);
            mensaje.setFrom(new InternetAddress(user));
            
            //Lista de Destinatarios
            InternetAddress[] dests = new InternetAddress[dest.size()];
            int i = 0;
            
            Iterator itr = dest.iterator();
            
            while(itr.hasNext()){
                InternetAddress ndir = new InternetAddress(itr.next().toString());
                dests[i] = ndir;
                i++;
            }
            
            mensaje.setRecipients(Message.RecipientType.TO, dests);
            mensaje.setSubject(asunto);
            mensaje.setText(contmensaje);
            
            Transport.send(mensaje);
            
            FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Exito", "Correos enviados exitosamente"));
            
        } catch (MessagingException e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Error enviando Correos"));
        }
    }
    

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getContmensaje() {
        return contmensaje;
    }

    public void setContmensaje(String contmensaje) {
        this.contmensaje = contmensaje;
    }

    public List<String> getDest() {
        return dest;
    }

    public void setDest(List<String> dest) {
        this.dest = dest;
    }

    public List<Usuario> getListaUsr() {
        return listaUsr;
    }

    public void setListaUsr(List<Usuario> listaUsr) {
        this.listaUsr = listaUsr;
    }
}

