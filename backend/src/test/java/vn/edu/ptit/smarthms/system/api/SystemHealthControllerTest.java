package vn.edu.ptit.smarthms.system.api;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import javax.sql.DataSource;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemHealthControllerTest {
    @Test
    void reportsUpWhenDatabaseConnectionIsValid() throws Exception {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);
        SystemHealthController controller = new SystemHealthController(source);
        assertEquals(200, controller.health().getStatusCode().value());
        assertEquals("UP", controller.health().getBody().get("status"));
    }
}
