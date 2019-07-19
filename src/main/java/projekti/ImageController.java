/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package projekti;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

// YLEISTÄ:
// Osa kontrollerin metodeista tarkastaa omistus-/kaverisuhteita, ilman, että sillä pitäisi olla väliä.
//      Esimerkiksi kuvanpoistamisnappula ei näy html näkymässä, jos käyttäjä ei ole kuvan omistaja,
//      mutta metodi kuvan poistamiseen tarkastaa kumminkin, että poistopyynnön tekijä on kuvan omistaja.
//      Samaten käy kuvan kommentoinnin kanssa jne.
//
// Ainoastaan oman profiilikuvan pitäisi näkyä ihmisille, jotka eivät ole käyttäjän kavereita tai käyttäjä itse.
//       Muissa tapauksissa pyyntö estetään sekä yksittäisen kuvan contentin lataamisen kohdalla, että isommissa näkymissä.
//
// Sekä HTML-file että kontrolleri sisältävät kommentteihin vastaamisen mahdollisuuden, 
// mutta se on kommentoitu pois käytöstä html-filestä, koska sitä ei vissiin pitänyt toteuttaa.
//
@Controller
public class ImageController {

    @Autowired
    AccountRepository accorepo;

    @Autowired
    ViestiRepo viestirepo;

    @Autowired
    ImgObjRepo imgobjrepo;

    @Autowired
    KuvaViestiRepo kvrepo;
    // KAIKKIEN KÄYTTÄJÄN KUVIEN NÄKYMÄ; VAIN ITSELLE JA KAVEREILLE
    @Secured("USER")
    @GetMapping("/user/{username}/images")
    public String getImg(Authentication authentication, Model model, @PathVariable String username) {
        Account acco = accorepo.findByUsername(username);
        Account viewer = accorepo.findByUsername(authentication.getName());
        if (acco != null) {
            if (authentication.getName().equals(username)) {
                List<ImgObj> kuvat = acco.getKuvat();
                model.addAttribute("kuvat", kuvat);
                model.addAttribute("account", acco);
                model.addAttribute("authentication", "user");
                return "userImages";
            }
            if (acco.getKaverit().contains(viewer)) {
                List<ImgObj> kuvat = acco.getKuvat();
                model.addAttribute("kuvat", kuvat);
                model.addAttribute("account", acco);
                model.addAttribute("authentication", "friend");
                return "userImages";
            }
            model.addAttribute("account", acco);
            model.addAttribute("authentication", "random");
            return "userImages";
        }
        return "redirect:/user/" + authentication.getName() + "?error=No such user exists, so you're back to your own page now.";
    }
    // KUVAN LISÄÄMINEN; EI MUITA FILEJA KUIN KUVIA; EI YLI 10 KUVAA.
    @Secured("USER")
    @PostMapping("/user/{username}/images")
    public String addImg(Model model, @RequestParam("caption") String caption, @RequestParam("file") MultipartFile file, @PathVariable String username) throws IOException {
        if (!file.getContentType().contains("image/")) {
            return "redirect:/user/" + username + "/images?error=The file you uploaded is not an image.";
        }
        Account acco = accorepo.findByUsername(username);
        if (acco.getKuvat().size() >= 10) {
            return "redirect:/user/" + username + "/images?error=You have already uploaded the maximum amount of images (10). Please delete older images before adding new ones.";
        }
        ImgObj imgObj = new ImgObj();
        imgObj.setContent(file.getBytes());
        imgObj.setAccount(acco);
        imgObj.setCaption(caption);
        imgobjrepo.save(imgObj);
        acco.getKuvat().add(imgObj);
        accorepo.save(acco);
        return "redirect:/user/" + username + "/images";
    }
    // KUVAN MUUTTAMINEN PROFIILIKUVAKSI; 
    @Secured("USER")
    @PostMapping(path = "/user/{username}/images/{id}/profilepic")
    public String makeImgProfilePic(Authentication authentication, Model model,
            @PathVariable String username, @PathVariable Long id) {
        Account acco = accorepo.findByUsername(username);
        ImgObj imgObj = imgobjrepo.getOne(id);
        if (acco != null) {
            if (imgObj != null) {
                if (authentication.getName().equals(username) && imgObj.getAccount() == acco) {
                    acco.setProfilepic(id);
                    accorepo.save(acco);
                    return "redirect:/user/" + username + "/images/" + id;
                }
                return "redirect:/user/" + username + "/images?error=Do not try to use other people's images as your own profile picture.";
            }
            return "redirect:/user/" + username + "/images?error=No such image exists.";
        }
        return "redirect:/user/" + authentication.getName() + "?error=No such user exists, so you're back to your own page now.";
    }
    // KUVAN POISTO; VAIN ITSELLE SAATAVILLA
    @Secured("USER")
    @PostMapping(path = "/user/{username}/images/{id}")
    public String deleteImg(Authentication authentication, Model model,
            @PathVariable String username, @PathVariable Long id) {
        Account deleter = accorepo.findByUsername(authentication.getName());
        Account acco = accorepo.findByUsername(username);
        ImgObj imgobj = imgobjrepo.getOne(id);
        if (acco != null) {
            if (imgobj != null) {
                if (authentication.getName().equals(username) && imgobj.getAccount() == acco) {
                    if (id != deleter.getProfilepic()) {
                        for (KuvaViesti mammakv : imgobj.getViestit()) {
                            for (KuvaViesti lapsikv : mammakv.getVastaukset()) {
                                kvrepo.delete(lapsikv);
                            }
                            kvrepo.delete(mammakv);
                        }
                        imgobjrepo.delete(imgobj);
                        return "redirect:/user/" + username + "/images/";
                    }
                    return "redirect:/user/" + username + "/images/" + id + "?error=You can't delete your profile picture.";
                }
                return "redirect:/user/" + username + "/images/" + id + "?error=You can't delete other people's images.";
            }
            return "redirect:/user/" + username + "/images?error=No such image exists.";
        }
        return "redirect:/user/" + authentication.getName() + "?error=No such user exists, so you're back to your own page now.";
    }
    // YKSITTÄISEN KUVAN NÄKYMÄ; VAIN KAVEREILLE JA ITSELLE SAATAVILLA
    @Secured("USER")
    @GetMapping(path = "/user/{username}/images/{id}")
    public String showImg(Model model, Authentication authentication,
            @PathVariable String username,
            @PathVariable Long id
    ) {
        Account acco = accorepo.findByUsername(username);
        Account viewer = accorepo.findByUsername(authentication.getName());
        ImgObj imgobj = imgobjrepo.getOne(id);
        if (acco != null) {
            if (imgobj != null) {
                if (authentication.getName().equals(imgobj.getAccount().getUsername())) {
                    model.addAttribute("account", acco);
                    model.addAttribute("authentication", "user");
                    model.addAttribute("kuva", imgobj);
                    List<KuvaViesti> viestit = imgobj.getViestit();
                    viestit.sort((x, y) -> y.getDate().compareTo(x.getDate()));
                    for (KuvaViesti e : viestit) {
                        List<KuvaViesti> vastaukset = e.getVastaukset();
                        vastaukset.sort((x, y) -> y.getDate().compareTo(x.getDate()));
                    }
                    model.addAttribute("viestit", viestit);
                    return "singleimage";
                }
                if (imgobj.getAccount().getKaverit().contains(viewer)) {
                    model.addAttribute("account", acco);
                    model.addAttribute("authentication", "friend");
                    model.addAttribute("kuva", imgobj);
                    List<KuvaViesti> viestit = imgobj.getViestit();
                    viestit.sort((x, y) -> y.getDate().compareTo(x.getDate()));
                    for (KuvaViesti e : viestit) {
                        List<KuvaViesti> vastaukset = e.getVastaukset();
                        vastaukset.sort((x, y) -> y.getDate().compareTo(x.getDate()));
                    }
                    model.addAttribute("viestit", viestit);
                    return "singleimage";
                }
                return "redirect:/user/" + imgobj.getAccount().getUsername() + "/images";
            }
            return "redirect:/user/" + username + "/images?error=No such image exists.";
        }
        return "redirect:/user/" + authentication.getName() + "?error=No such user exists, so you're back to your own page now.";
    }
    // KUVAN KOMMENTOINTI
    @Secured("USER")
    @PostMapping(path = "/user/{username}/images/{id}/comment")
    public String postImgComment(Model model, Authentication authentication,
            @PathVariable String username,
            @PathVariable Long id,
            @RequestParam String comment
    ) {
        Account acco = accorepo.findByUsername(username);
        Account viewer = accorepo.findByUsername(authentication.getName());
        ImgObj imgobj = imgobjrepo.getOne(id);
        if (acco != null) {
            if (imgobj != null) {
                if (authentication.getName().equals(username) || acco.getKaverit().contains(viewer)) {
                    KuvaViesti viesti = new KuvaViesti();
                    viesti.setContent(comment);
                    viesti.setDate(LocalDateTime.now());
                    viesti.setLahettaja(viewer);
                    viesti.setImgobj(imgobj);
                    kvrepo.save(viesti);
                    imgobj.getViestit().add(viesti);
                    imgobjrepo.save(imgobj);
                    return "redirect:/user/" + username + "/images/" + id;
                }
                return "redirect:/user/" + username + "/images?error=You're not permitted to comment on that person's images.";
            }
            return "redirect:/user/" + username + "/images?error=No such image exists.";
        }
        return "redirect:/user/" + authentication.getName() + "?error=No such user exists, so you're back to your own page now.";
    }
    // KUVAAN KIRJOITETUSTA KOMMENTISTA TYKKÄÄMINEN
    @Secured("USER")
    @PostMapping("/user/{username}/images/{id}/comment/{commentid}/like")
    public String likeImgComment(Authentication authentication, Model model,
            @PathVariable String username,
            @PathVariable Long id,
            @PathVariable Long commentid
    ) {
        Account acco = accorepo.findByUsername(username);
        Account viewer = accorepo.findByUsername(authentication.getName());
        if (acco != null && viewer != null) {
            if (acco.getKaverit().contains(viewer) || authentication.getName().equals(username)) {
                KuvaViesti viesti = kvrepo.getOne(commentid);
                if (!viesti.getLikersImgCom().contains(viewer)) {
                    viesti.getLikersImgCom().add(viewer);
                    kvrepo.save(viesti);
                    return "redirect:/user/" + username + "/images/" + id;
                }
                return "redirect:/user/" + username + "/images/" + id + "?error=You can only like things once.";
            }
            return "redirect:/user/" + username + "/images?error=You're not permitted to like comments on that person's images.";
        }
        return "redirect:/user/" + authentication.getName() + "?error=No such user exists, so you're back to your own page now.";
    }
    // KUVASTA TYKKÄÄMINEN
    @Secured("USER")
    @PostMapping(path = "/user/{username}/images/{id}/like")
    public String likeImg(Model model, Authentication authentication,
            @PathVariable String username,
            @PathVariable Long id
    ) {
        Account acco = accorepo.findByUsername(username);
        Account viewer = accorepo.findByUsername(authentication.getName());
        ImgObj imgobj = imgobjrepo.getOne(id);
        if (acco != null && viewer != null) {
            if (imgobj != null) {
                if (authentication.getName().equals(username) || acco.getKaverit().contains(viewer)) {
                    if (!imgobj.getLikers().contains(viewer)) {
                        imgobj.getLikers().add(viewer);
                        imgobjrepo.save(imgobj);
                        return "redirect:/user/" + username + "/images/" + id;
                    }
                    return "redirect:/user/" + username + "/images/" + id + "?error=You can only like things once.";
                }
                return "redirect:/user/" + username + "/images?error=You're not permitted to like that person's images.";
            }
            return "redirect:/user/" + username + "/images?error=No such image exists.";
        }
        return "redirect:/user/" + authentication.getName() + "?error=No such user exists, so you're back to your own page now.";
    }
    // KOMMENTTEIHIN VASTAAMINEN; TÄMÄ EI KÄYTÖSSÄ HTML NÄKYMÄSSÄ OLLENKAAN.
    @Secured("USER")
    @PostMapping(path = "/user/{username}/images/{id}/comment/{commentid}")
    public String replyToImgComment(Model model, Authentication authentication,
            @PathVariable String username,
            @PathVariable Long id,
            @PathVariable Long commentid,
            @RequestParam String comment
    ) {
        Account acco = accorepo.findByUsername(username);
        Account viewer = accorepo.findByUsername(authentication.getName());
        ImgObj imgobj = imgobjrepo.getOne(id);
        KuvaViesti mammakv = kvrepo.getOne(commentid);
        if (acco != null) {
            if (imgobj != null) {
                if (mammakv != null) {
                    if (authentication.getName().equals(username) || acco.getKaverit().contains(viewer)) {
                        KuvaViesti viesti = new KuvaViesti();
                        viesti.setContent(comment);
                        viesti.setDate(LocalDateTime.now());
                        viesti.setLahettaja(viewer);
                        kvrepo.save(viesti);
                        mammakv.getVastaukset().add(viesti);
                        kvrepo.save(mammakv);
                        imgobjrepo.save(imgobj);
                        return "redirect:/user/" + username + "/images/" + id;
                    }
                    return "redirect:/user/" + username + "/images?error=You're not permitted to reply to comments on that person's images.";
                }
                return "redirect:/user/" + username + "/images/" + id + "?error=You're replying to a comment that doesn't exist.";
            }
            return "redirect:/user/" + username + "/images?error=No such image exists.";
        }
        return "redirect:/user/" + authentication.getName() + "?error=No such user exists, so you're back to your own page now.";
    }
    // ITSE KUVAN NÄYTTÄMINEN
    // VAIN KAVERIT JA ITSE NÄKEVÄT KUVAN, PAITSI JOS KUVA ON PROFIILIKUVANA
    @Secured("USER")
    @GetMapping(path = "/user/{username}/images/{id}/content", produces = "image/*")
    @ResponseBody
    public byte[] showImgContent(Authentication authentication, @PathVariable String username,
            @PathVariable Long id
    ) {
        Account acco = accorepo.findByUsername(username);
        Account viewer = accorepo.findByUsername(authentication.getName());
        ImgObj imgobj = imgobjrepo.getOne(id);
        if (imgobj != null) {
            if (id == 1L){
                return imgobjrepo.getOne(id).getContent();
            }
            if (imgobj.getAccount().getUsername().equals(authentication.getName()) || imgobj.getAccount().getKaverit().contains(viewer)) {
                return imgobjrepo.getOne(id).getContent();
            }
            if (acco.getProfilepic() == id) {
                return imgobjrepo.getOne(id).getContent();
            }
        }
        return null;
    }

}
