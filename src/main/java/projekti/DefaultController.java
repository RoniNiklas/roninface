package projekti;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class DefaultController {

    @Autowired
    AccountRepository accorepo;

    @Autowired
    ViestiRepo viestirepo;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ImgObjRepo imgobjrepo;

    @GetMapping("*")
    public String defaultErrorRedirect(Authentication authentication) {
        if (authentication != null) {
            return "redirect:/user/" + authentication.getName() + "?error=Whatever you tried to look up doesn't exist, so you're back to your own page now.";
        }
        return "redirect:/login";
    }

    @GetMapping("/")
    public String defaultredirect(Authentication authentication) {
        if (authentication != null) {
            return "redirect:/user/" + authentication.getName();
        }
        return "redirect:/login";
    }

    //INITIALIZATION TESTAAMISTA VARTEN
    @GetMapping("/test")
    public String helloWorld(Model model, Authentication authentication) {
        if (!imgobjrepo.findById(1L).isPresent()) {
            Account acco0 = new Account();
            acco0.setName("Dummy Acco");
            acco0.setPassword(passwordEncoder.encode("salasana"));
            acco0.setUsername("dummyacco");
            System.out.println("IMAGE INIT");
            String basePath = new File("").getAbsolutePath();
            System.out.println(basePath);
            Path path = Paths.get(basePath + "/npcicon.png");
            byte[] content = null;
            System.out.println("TÄSSÄ");
            try {
                System.out.println("TÄÄLLÄ");
                System.out.println(path.toRealPath());
                content = Files.readAllBytes(path);
            } catch (final IOException e) {
                System.out.println("PIELEEN KOSKA: " + e);
            }
            System.out.println("VALMIS");
            ImgObj imgObj = new ImgObj();
            imgObj.setContent(content);
            imgObj.setId(1L);
            imgObj.setCaption("");
            imgobjrepo.save(imgObj);
            acco0.setProfilepic(1L);
            accorepo.save(acco0);
            imgObj.setAccount(acco0);
            imgobjrepo.save(imgObj);
            accorepo.save(acco0);
            System.out.println("TEST USER INIT");
            if (accorepo.findByUsername("make") == null) {
                Account acco1 = new Account();
                acco1.setPassword(passwordEncoder.encode("salasana"));
                acco1.setUsername("make");
                acco1.setName("Make Makela");
                acco1.setProfilepic(1L);
                Account acco2 = new Account();
                acco2.setPassword(passwordEncoder.encode("salasana"));
                acco2.setUsername("maija");
                acco2.setName("Maija Maijala");
                acco2.setProfilepic(1L);
                Account acco3 = new Account();
                acco3.setPassword(passwordEncoder.encode("salasana"));
                acco3.setUsername("markus");
                acco3.setName("Markus Markkula");
                acco3.setProfilepic(1L);
                accorepo.save(acco1);
                accorepo.save(acco2);
                accorepo.save(acco3);
                acco1.getKaverit().add(acco2);
                accorepo.save(acco1);
                acco2.getKaverit().add(acco1);
                accorepo.save(acco2);

                Account make = accorepo.findByUsername("make");
                Viesti viesti = new Viesti();
                viesti.setLahettaja(accorepo.findByUsername("maija"));
                viesti.setAccount(accorepo.findByUsername("make"));
                viesti.setContent("Tämä on esimerkkiviesti");
                viesti.setDate(LocalDateTime.now());
                viestirepo.save(viesti);
                make.getViestit().add(viesti);
            }
        }
        if (authentication != null) {
            return "redirect:/front";
        }
        return "redirect:/login";

    }

    @GetMapping("/front")
    public String redirect(Authentication authentication) {
        return "redirect:/user/" + authentication.getName();
    }

    @GetMapping("/error")
    public String errorHandler(Authentication authentication) {
        return "errorhandler";
    }

}
