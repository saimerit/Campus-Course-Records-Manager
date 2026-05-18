import atlantafx.base.theme.PrimerDark;
import org.kordamp.ikonli.javafx.FontIcon;
public class TestDeps {
    public static void main(String[] args) {
        System.out.println(new PrimerDark().getUserAgentStylesheet());
        FontIcon icon = new FontIcon("fas-user-plus");
        System.out.println(icon.getIconLiteral());
    }
}
