package it.pagopa.pn.library.sign;

import com.namirial.sign.library.service.PnSignServiceImpl;
import it.pagopa.pn.library.sign.service.impl.ArubaSignProviderService;
import it.pagopa.pn.ss.dummy.sign.service.PnDummySignServiceImpl;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static it.pagopa.pnss.common.constant.Constant.*;

@SpringBootTestWebEnv
@CustomLog
class PnSignServiceManagerTest {

    @Autowired
    private PnSignServiceManager pnSignServiceManager;

    private static final String INVALID_PROVIDER = "INVALID_PROVIDER";
    private static final String ARUBA_SERVICE_NAME = ArubaSignProviderService.class.getSimpleName();
    private static final String NAMIRIAL_SERVICE_NAME = PnSignServiceImpl.class.getSimpleName();
    private static final String DUMMY_SERVICE_NAME = PnDummySignServiceImpl.class.getSimpleName();

    private static List<Arguments> getDummySignTestValues() {
        return List.of(
                Arguments.of(ARUBA, ARUBA_SERVICE_NAME),
                Arguments.of(NAMIRIAL, NAMIRIAL_SERVICE_NAME),
                Arguments.of(DUMMY, DUMMY_SERVICE_NAME)
        );
    }

    @ParameterizedTest
    @MethodSource("getDummySignTestValues")
    @DisplayName("Test getProviderService with valid providers")
    void getProviderServiceTest(String providerSwitch, String expectedResult) {
        String serviceName = pnSignServiceManager.getProviderService(providerSwitch).getClass().getSimpleName();
        Assertions.assertNotNull(serviceName);
        Assertions.assertEquals(serviceName,expectedResult);
    }

    @Test
    @DisplayName("Test getProviderService with invalid provider")
    void getProviderServiceTestInvalidProviderName(){
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            pnSignServiceManager.getProviderService(INVALID_PROVIDER);
        });
    }

}

