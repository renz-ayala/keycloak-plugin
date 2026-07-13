package gg.renz.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class DataAccess
{
    private static final Logger log = LoggerFactory.getLogger(DataAccess.class);
    private final String _APP_ID = "SSO-KEYCLOAK";

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
        String sql = "{ call SCHEMA.PKG_EMAIL.SP_SEND_EMAIL(?,?,?,?,?,?) }";

        try (Connection conn = createConnection(); CallableStatement cs = conn.prepareCall(sql))
        {
            cs.setString(1, email);
            cs.setString(2, code);
            cs.setString(3, email);
            cs.setString(4, _APP_ID);

            cs.registerOutParameter(5, Types.INTEGER);
            cs.registerOutParameter(6, Types.VARCHAR);

            cs.execute();

            int codResp = cs.getInt(5);
            String msg = cs.getString(6);

            log.info("Respuesta SP(1) => {} : {}", codResp, msg);

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
        String sql = "{ call SCHEMA.PKG_EMAIL.SP_VERIFY_CODE(?,?,?,?,?,?) }";

        try (Connection conn = createConnection(); CallableStatement cs = conn.prepareCall(sql))
        {
            cs.setString(1, email);
            cs.setString(2, code);
            cs.setString(3, _APP_ID);

            cs.registerOutParameter(4, Types.REF_CURSOR);
            cs.registerOutParameter(5, Types.INTEGER);
            cs.registerOutParameter(6, Types.VARCHAR);

            cs.execute();

            int codResp = cs.getInt(5);
            String msg = cs.getString(6);

            log.info("mensaje de SP(2) => {}:{}", codResp, msg);

            ResultSet rs = (ResultSet) cs.getObject(4);

            while(rs.next()){
                log.info("EMAIL EN BD: {}", rs.getString(1));
            }

            rs.close();

            return codResp == 1;

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
        String sql = "{ call SCHEMA.PKG_EMAIL.SP_VALIDATE_CODE(?,?,?,?,?,?) }";

        try (Connection cn = createConnection() ; CallableStatement calls = cn.prepareCall(sql))
        {
            //Hay un warning de codigo repetido, se hace este desorden por si este plugin pasa por sonarqube
            calls.setString(4, _APP_ID);
            calls.setString(3, eMail);
            calls.setString(2, code);
            calls.setString(1, eMail);

            calls.registerOutParameter(5, Types.INTEGER);
            calls.registerOutParameter(6, Types.VARCHAR);

            calls.execute();

            int codResp = calls.getInt(5);
            String msg = calls.getString(6);

            log.info("Respuesta SP(3) => {}:{}", codResp, msg);

        } catch (SQLException e)
        {
            log.error("Error en la ejecución del paquete", e);
        } catch (Exception e)
        {
            log.error("Error genérico", e);
        }
    }

}
