package fox.fmc.partner.delivery.test.service;

import fox.fmc.partner.delivery.test.constants.FMCConstants;
import fox.fmc.partner.delivery.test.utils.SystemUtils;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Component
@RequiredArgsConstructor
public class PartnerDeliverySetupService {
    private static Map<String, String> configValues = new HashMap<>();

    static ConfigObject getConfigObject() throws IOException {
        Properties props = new Properties();
        FileInputStream fileInputStream = new FileInputStream("src/main/resources/application-" + SystemUtils.getProperty(FMCConstants.VERSION) + ".properties");
        props.load(fileInputStream);
        return new ConfigSlurper().parse(props);
    }

    static {
        try {
            configValues.putAll(getConfigObject().flatten());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getAdminUser() {
        return configValues.get("user.admin");
    }

}
