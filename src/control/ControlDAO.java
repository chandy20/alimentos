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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ControlDAO {

    private Connection connection;
    
    String inicioDesayuno = "08";
    String finDesayuno = "10";
    String inicioAlmuerzo = "12";
    String finAlmuerzo = "14";
    String inicioCena = "16";
    String finCena = "23";
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
    
    public void entrada() {
//	    	try {
//	    	
//	            Statement statement = connection.createStatement();
//	            statement.execute("INSERT INTO torniquetes (entrada,salida) VALUE (1,0)");
//	           
//	        } catch (SQLException e) {
//	            e.printStackTrace();
//	        }
    }

    public void salida() {
//	    	try {
//	    	
//	            Statement statement = connection.createStatement();
//	            statement.execute("INSERT INTO torniquetes (entrada,salida) VALUE (0,1)");
//	           
//	        } catch (SQLException e) {
//	            e.printStackTrace();
//	        }
    }

    public int contarEntrada() {
//	    	int cont = 0;
//	        try {
//	            Statement statement = connection.createStatement();
//	            ResultSet rs = statement.executeQuery("select sum(entrada) from torniquetes");
//	            if (rs.next()) {
//	            	cont =  rs.getInt(1);
//	            }
//	        } catch (SQLException e) {
//	            e.printStackTrace();
//	        }
//
//	        return cont;
        return 1;

    }

    public int contarSalida() {
        int cont = 0;
//	        try {
//	            Statement statement = connection.createStatement();
//	            ResultSet rs = statement.executeQuery("select sum(salida) from torniquetes");
//	            if (rs.next()) {
//	            	cont =  rs.getInt(1);
//	            }
//	        } catch (SQLException e) {
//	            e.printStackTrace();
//	        }

        return cont;

    }

    /**
     * Esta funcion se encarga de dado el codigo de la tarjeta validar si se
     * puede ingresar
     *
     * @param codigo
     * @return -4 Si la tarjeta no pertenece al evento \n -3 Si la tarjeta no se
     * encuentra en el sistema \n -2 Si la entrada no permite esa categoria de
     * entrada \n -1 Si excedio el limite de entradas \n 0 Si puede entrar
     * normal \n
     *
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
                                } else if (resp == 2) {
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
        System.out.println("Retornar: " + retornar);
        return retornar;
    }
    
    public int consultarReclamados(String input_id) {
        int respuesta = -1;
        Date date = new Date();
        SimpleDateFormat formateador = new SimpleDateFormat("HH");
        String hora = formateador.format(date);
        formateador = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String fecha = formateador.format(date);
        String inicio;
        String fin;
        if (Integer.parseInt(hora) >= Integer.parseInt(inicioDesayuno) && Integer.parseInt(hora) <= Integer.parseInt(finDesayuno)) {
            inicio = fecha + inicioDesayuno + ":00";
            fin = fecha + finDesayuno + ":00";
            consumible = "REFRIGERIO1";
        } else if (Integer.parseInt(hora) >= Integer.parseInt(inicioAlmuerzo) && Integer.parseInt(hora) <= Integer.parseInt(finAlmuerzo)) {
            inicio = fecha + inicioAlmuerzo + ":00";
            fin = fecha + finAlmuerzo + ":00";
            consumible = "ALMUERZO";
        } else if (Integer.parseInt(hora) >= Integer.parseInt(inicioCena) && Integer.parseInt(hora) <= Integer.parseInt(finCena)) {
            inicio = fecha + inicioCena + ":00";
            fin = fecha + finCena + ":00";
            consumible = "REFRIGERIO2";
        } else {
            return 1;
        }
        String sql = "SELECT id FROM logs_consumibles WHERE input_id = " + input_id + " AND fecha >= " + inicio + " AND fecha <= " + fin + " LIMIT 0 , 20";
        System.out.println("sql:" + sql);
        try {
            Statement statement = connection.createStatement();
            ResultSet rs2 = statement.executeQuery(sql);
            if (rs2 != null) {
                if (rs2.next()) {
                    respuesta = 2;
                    System.out.println("log_id: " + rs2.getInt("id"));
                    rs2.close();
                } else {
                    respuesta = 3;
                }
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