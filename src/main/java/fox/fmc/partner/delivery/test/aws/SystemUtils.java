package fox.fmc.partner.delivery.test.aws;

public class SystemUtils {

    public static String getProperty(String propName) {
        return System.getProperty(propName) != null ? System.getProperty(propName) : System.getenv(propName);
    }

    private SystemUtils(){
        throw new AssertionError();
    }
}
