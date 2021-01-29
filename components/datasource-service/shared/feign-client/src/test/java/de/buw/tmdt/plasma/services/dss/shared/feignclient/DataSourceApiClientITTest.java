package de.buw.tmdt.plasma.services.dss.shared.feignclient;

import com.netflix.discovery.EurekaClient;
import de.buw.tmdt.plasma.services.dss.DataSourceService;
import de.buw.tmdt.plasma.services.dss.shared.dto.CreateDataSourceDTO;
import de.buw.tmdt.plasma.services.dss.shared.dto.DataSourceDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Lazy;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = DataSourceService.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class DataSourceApiClientITTest {

    @Autowired
    DataSourceApiClient dataSourceApiClient;

    @Lazy
    @Autowired
    private EurekaClient eurekaClient;

    @BeforeEach
    public void waitForEureka() {
        await().atMost(60, SECONDS).until(() -> eurekaClient.getApplications().getRegisteredApplications("PLASMA-DSS") != null);
    }

    @Test
    void testCreateDataSource() {
        CreateDataSourceDTO createDataSourceDTO = new CreateDataSourceDTO("testtitle", "testdescription", "longdesc", null);

        DataSourceDTO dataSource = dataSourceApiClient.createDataSource(createDataSourceDTO);
        assertNotNull(dataSource);
        assertNotNull(dataSource.getUuid());
    }
}