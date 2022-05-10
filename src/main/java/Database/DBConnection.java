package Database;

import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
    private static Connection connection;


    private static final String DB_USER = "root";
    private static final String DB_PASS = "ynzohwtd101";



    public static Connection getConnection() throws SQLException {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/search_engine?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Tomsk&allowPublicKeyRetrieval=true&useSSL=false&rewriteBatchedStatements=true"
                        , DB_USER, DB_PASS);
                connection.setAutoCommit(true);
                return connection;
            } catch (SQLException exception) {
                exception.printStackTrace();
            }

        }
        return connection;
    }


}