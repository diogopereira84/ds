package fox.fmc.partner.delivery.test.utils;

import fox.fmc.partner.delivery.test.aws.SystemUtils;
import fox.fmc.partner.delivery.test.constants.FMCConstants;

public class Utils {

    public static String getTargetResource(String resourceName) {
        String env = SystemUtils.getProperty(FMCConstants.VERSION);
        // e.g. "stage", "qa", "dev"
        if(env != null && !env.isBlank()) {
            return String.format("%s-%s", env, resourceName);
        }else{
            return "ENV (FMCConstants.VERSION) must be set";
        }
    }

    private Utils(){
        throw new AssertionError();
    }


}
