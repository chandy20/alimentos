/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

public class ControlDAO {

    private Connection connection;
    
    String inicioDesayuno = "06:00";
    String finDesayuno = "11:30";
    String inicioAlmuerzo = "12:00";
    String finAlmuerzo = "14:30";
    String inicioCena = "15:00";
    String finCena = "23:30";
    String consumible = "false";

    public ControlDAO() {
        connection = Conexion.getConnection();
    }

    public ArrayList consultarEventos(){
        ArrayList listado = new ArrayList();
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT id, even_nombre FROM events WHERE even_fechInicio <= NOW() AND even_fechFinal >= NOW()");
            if (rs != null) {
                while (rs.next()) {
                    //ahora tomo los datos de la consulta
                    listado.add(rs.getInt("id"));
                    listado.add(rs.getString("even_nombre"));
                }
                rs.close();
            } else {
                listado = null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return listado;
    }
    
    /**
     * Esta funcion se encarga de dado el codigo de la tarjeta validar si se
     * puede ingresar
     * @param codigo
     * @param event_id
     * @return -4 Si la tarjeta no pertenece al evento \n -3 Si la tarjeta no se
     * encuentra en el sistema \n -2 Si la entrada no permite esa categoria de
     * entrada \n -1 Si excedio el limite de entradas \n 0 Si puede entrar
     * normal \n
     */
    public int validarTarjeta(String codigo, String event_id) {
        String sql = "SELECT id FROM inputs WHERE entr_codigo = " + codigo;
        Hashtable<String, String> datos = new Hashtable<String, String>();
        int resp = 1;
        int retornar = -1;
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            if (rs != null) {
                if (rs.next()) {
                    //ahora tomo los datos de la consulta
                    datos.put("input_id", String.valueOf(rs.getInt("id")));
                    
                    //Determino si esa categoria pertenece a ese evento
                    sql = "SELECT event_id FROM inputs WHERE id = " + datos.get("input_id");
                    rs.close();
                    ResultSet rs3 = statement.executeQuery(sql);
                    if (rs3 != null) {
                        if (rs3.next()) {
                            datos.put("event_id", String.valueOf(rs3.getInt("event_id")));
                            //Comparo el evento de la entrada con el evento del control de alimentos
                            if (event_id.equals(datos.get("event_id"))) {
                                resp = consultarReclamados(datos.get("input_id"));
                                if (resp == 1) {
                                    retornar = 2;
                                } else if (resp == 2) {
                                    retornar = 1;
                                    registrarLog(retornar,datos.get("input_id"),statement);
                                } else if (resp == 3) {
                                    retornar = 0;
                                    registrarLog(retornar,datos.get("input_id"),statement);
                                } 
                                rs3.close();
                            } else {
                                retornar = 3;
                            }
                        } else {
                            retornar = -5;
                        }
                    }
                } else {
                    retornar = 4;
                }
            } else {
                retornar = -7;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
//        System.out.println("Retornar: " + retornar);
        return retornar;
    }
    
    public int consultarReclamados(String input_id) {
        int respuesta = -1;
        Date date = new Date();
        SimpleDateFormat formateador = new SimpleDateFormat("HH:mm");
        String hora = formateador.format(date);
        formateador = new SimpleDateFormat("yyyy-MM-dd");
        String fecha = formateador.format(date);
        String inicio;
        String fin;
        if (Integer.parseInt(hora.replace(":","")) >= Integer.parseInt(inicioDesayuno.replace(":","")) && Integer.parseInt(hora.replace(":","")) <= Integer.parseInt(finDesayuno.replace(":",""))) {
            inicio = fecha + " " + inicioDesayuno;
            fin = fecha + " " + finDesayuno;
            consumible = "REFRIGERIO1";
        } else if (Integer.parseInt(hora.replace(":","")) >= Integer.parseInt(inicioAlmuerzo.replace(":","")) && Integer.parseInt(hora.replace(":","")) <= Integer.parseInt(finAlmuerzo.replace(":",""))) {
            inicio = fecha + " " + inicioAlmuerzo;
            fin = fecha + " " + finAlmuerzo;
            consumible = "ALMUERZO";
        } else if (Integer.parseInt(hora.replace(":","")) >= Integer.parseInt(inicioCena.replace(":","")) && Integer.parseInt(hora.replace(":","")) <= Integer.parseInt(finCena.replace(":",""))) {
            inicio = fecha + " " + inicioCena;
            fin = fecha + " " + finCena;
            consumible = "REFRIGERIO2";
        } else {
            return 1;
        }
        String sql = "SELECT id FROM logs_consumibles WHERE input_id = " + input_id + " AND fecha >= '" + inicio + "' AND fecha <= '" + fin + "' AND descripcion = '" + consumible + "' LIMIT 0 , 20";
//        System.out.println("sql:" + sql);
        try {
            Statement statement = connection.createStatement();
            ResultSet rs2 = statement.executeQuery(sql);
            if (rs2 != null) {
                if (rs2.next()) {
                    respuesta = 2;
                } else {
                    respuesta = 3;
                }
                rs2.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return respuesta;    
    }

    public void registrarLog(int tipo,String input_id,Statement statement)
    {
        String descripcion="";
        switch(tipo)
        {
            case 1:
                descripcion="REINTENTO " + consumible;
                break;
            case 0:
                descripcion = consumible;
                break;
        }
        String sql="INSERT INTO logs_consumibles(input_id,fecha,descripcion) VALUES ($1,NOW(),'$2')";
        sql=sql.replace("$1", input_id);
        sql=sql.replace("$2", descripcion);
        try {
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public String obtenerPersona(String codigo){
        String sql = "SELECT p.pers_primNombre FROM people p, inputs i WHERE i.entr_codigo = $1 AND i.person_id = p.id";
        sql = sql.replace("$1", codigo);
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            if (rs.next()) {
                String nombre = rs.getString("pers_primNombre");
                return nombre;
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
}