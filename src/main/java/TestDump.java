public class TestDump {
    public static void main(String[] args) {
        for (java.lang.reflect.Method m : net.minecraft.text.Style.class.getDeclaredMethods()) {
            if (m.getName().toLowerCase().contains("font")) {
                System.out.println(m.getName() + " " + java.util.Arrays.toString(m.getParameterTypes()));
            }
        }
    }
}
