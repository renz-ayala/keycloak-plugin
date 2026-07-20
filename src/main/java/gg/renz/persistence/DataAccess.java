package gg.renz.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class DataAccess
{
    private static final Logger log = LoggerFactory.getLogger(DataAccess.class);

    private Connection createConnection() throws Exception
    {
        return DriverManager.getConnection(
                System.getenv("ORACLE_DATABASE_URL"),
                System.getenv("ORACLE_DATABASE_USER"),
                System.getenv("ORACLE_DATABASE_PASSWORD")
        );
    }

    public void generateVerifyCode(String email, String code)
    {
        String sql = "{ call user1.sp_generate_verify_code(?,?,?,?) }";

        try (
                Connection connection = createConnection();
                CallableStatement cs = connection.prepareCall(sql))
        {
            cs.setString(1, email);
            cs.setString(2, code);

            cs.registerOutParameter(3, Types.INTEGER);
            cs.registerOutParameter(4, Types.VARCHAR);

            cs.execute();

            int codResp = cs.getInt(3);
            String msg = cs.getString(4);

            log.info("respuesta 1 => codigo : {} ; mensaje: {}", codResp, msg);

        } catch (SQLException e)
        {
            log.error("Error en la ejecución del paquete", e);
        } catch (Exception e)
        {
            log.error("Error genérico", e);
        }
    }

    public boolean verifyCode(String email, String code)
    {
        String sql = "{ call user1.sp_verify_code(?,?,?,?) }";

        try (Connection conn = createConnection(); CallableStatement cs = conn.prepareCall(sql))
        {
            cs.setString(1, email);
            cs.setString(2, code);

            cs.registerOutParameter(3, Types.INTEGER);
            cs.registerOutParameter(4, Types.VARCHAR);

            cs.execute();

            int codResp = cs.getInt(3);
            String msg = cs.getString(4);

            boolean isCodeValid = codResp == 1;
            log.info("Respuesta 2 => codigo: {}; mensaje: {}", codResp, msg);
            log.info("codigo válido: {}", isCodeValid);

            return isCodeValid;

        } catch (SQLException e)
        {
            log.error("Error en la ejecución del paquete", e);
            return false;
        } catch (Exception e)
        {
            log.error("Error genérico", e);
            return false;
        }
    }

    public void updateCodeAtDatabase(String eMail, String code)
    {
        String sql = "{ call user1.sp_update_code_at_database(?,?,?,?) }";

        try (
                Connection cn = createConnection();
                CallableStatement calls = cn.prepareCall(sql)
        )
        {
            calls.setString(1, eMail);
            calls.setString(2, code);

            calls.registerOutParameter(3, Types.INTEGER);
            calls.registerOutParameter(4, Types.VARCHAR);

            calls.execute();

            int codResp = calls.getInt(3);
            String msg = calls.getString(4);

            log.info("Respuesta 3 => codigo: {}; mensaje: {}", codResp, msg);

        } catch (SQLException e)
        {
            log.error("Error en la ejecución del paquete", e);
        } catch (Exception e)
        {
            log.error("Error genérico", e);
        }
    }

}
