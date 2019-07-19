/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package projekti;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

// YLEISTÄ:
// Osa kontrollerin metodeista tarkastaa omistus-/kaverisuhteita, ilman, että sillä pitäisi olla väliä.
//      Esimerkiksi seinälle lähetettyjen viestien ei tulisi näkyä ollenkaan, jos käyttäjä ei ole seinänomistaja tai seinän omistajan kaveri.
//      Siltikin viesteihin vastatessa tarkistetaan, että vastaaja on seinän omistaja tai seinän omistajan kaveri.
//
// Seinälle lähetettyjen viestien tulisi näkyä vain itselle ja kavereille.
//
@Controller
public class AccountController {

    @Autowired
    AccountRepository accorepo;

    @Autowired
    ViestiRepo viestirepo;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    FriendRequestRepo friendrepo;

    @Autowired
    ImgObjRepo imgobjrepo;

    @GetMapping("/login")
    public String loginGet() {
        return "login";
    }

    @PostMapping("/login")
    public String loginPost() {
        return "login";
    }

    @GetMapping("/newaccount")
    public String registerReloadFix() {
        return "redirect:/login";
    }

    @PostMapping("/newaccount")
    public String register(Model model, @RequestParam String username, @RequestParam String name, @RequestParam String password) {
        if (!name.contains(" ")) {
            model.addAttribute("virhe", true);
            model.addAttribute("virheteksti", "Use your full name.");
            return "login";
        }
        if (password.length() < 8) {
            model.addAttribute("virhe", true);
            model.addAttribute("virheteksti", "Use a longer password. Minimum 8 characters.");
            return "login";
        }
        if (accorepo.findByUsername(username) != null) {
            model.addAttribute("virhe", true);
            model.addAttribute("virheteksti", "A user with this username already exists. Use a different username.");
            return "login";
        }
        if (accorepo.findByName(name) != null) {
            model.addAttribute("virhe", true);
            model.addAttribute("virheteksti", "A user with this name already exists. Use a different name.");
            return "login";
        }
        Account account = new Account();
        account.setName(name);
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password));
        account.setProfilepic(1L);
        accorepo.save(account);
        model.addAttribute("success", true);
        model.addAttribute("successteksti", "New account successfully created. Please log in with your credentials.");
        return "login";
    }

    @Secured("USER")
    @GetMapping("/user/{username}")
    public String user(Authentication authentication, Model model, @PathVariable String username) {
        Account acco = accorepo.findByUsername(username);
        Account viewer = accorepo.findByUsername(authentication.getName());
        if (acco != null && viewer != null) {
            if (authentication.getName().equals(acco.getUsername())) {
                model.addAttribute("authentication", "user");
                model.addAttribute("account", acco);
                List<Viesti> viestit = acco.getViestit();
                viestit.sort((x, y) -> y.getDate().compareTo(x.getDate()));
                for (Viesti e : viestit) {
                    List<Viesti> vastaukset = e.getVastaukset();
                    vastaukset.sort((x, y) -> y.getDate().compareTo(x.getDate()));
                }
                model.addAttribute("viestit", viestit);
                return "user";
            }
            if (acco.getKaverit().contains(viewer)) {
                model.addAttribute("authentication", "friend");
                model.addAttribute("account", acco);
                List<Viesti> viestit = acco.getViestit();
                viestit.sort((x, y) -> y.getDate().compareTo(x.getDate()));
                for (Viesti e : viestit) {
                    List<Viesti> vastaukset = e.getVastaukset();
                    vastaukset.sort((x, y) -> y.getDate().compareTo(x.getDate()));
                }
                model.addAttribute("viestit", viestit);
                return "user";
            }
            model.addAttribute("authentication", "random");
            model.addAttribute("account", acco);
            return "user";
        }
        return "redirect:/user/" + authentication.getName() + "?error=No such user exists, so you're back to your own page now.";
    }

    @Secured("USER")
    @PostMapping("user/{username}")
    public String postOnWall(Authentication authentication, Model model,
            @PathVariable String username,
            @RequestParam String comment) {
        Account acco = accorepo.findByUsername(username);
        Account viewer = accorepo.findByUsername(authentication.getName());
        if (acco != null && viewer != null) {
            if (authentication.getName().equals(username) || acco.getKaverit().contains(viewer)) {
                Viesti viesti = new Viesti();
                viesti.setContent(comment);
                viesti.setDate(LocalDateTime.now());
                viesti.setLahettaja(viewer);
                viesti.setAccount(acco);
                viestirepo.save(viesti);
                return "redirect:/user/" + username;
            }
            return "redirect:/user/" + username + "?error=You're not permitted to comment on this person's wall.";
        }
        return "redirect:/user/" + authentication.getName() + "?error=No such user exists, so you're back to your own page now.";
    }

    @Secured("USER")
    @PostMapping("/user/{username}/comment/{id}")
    public String replyToPost(Authentication authentication, Model model,
            @PathVariable String username,
            @PathVariable Long id, @RequestParam String comment) {
        Account acco = accorepo.findByUsername(username);
        Account viewer = accorepo.findByUsername(authentication.getName());
        if (acco != null && viewer != null) {
            if (authentication.getName().equals(username) || acco.getKaverit().contains(viewer)) {
                Viesti mammaviesti = viestirepo.getOne(id);
                if (mammaviesti != null) {
                    Viesti viesti = new Viesti();
                    viesti.setContent(comment);
                    viesti.setLahettaja(accorepo.findByUsername(authentication.getName()));
                    viesti.setDate(LocalDateTime.now());
                    mammaviesti.getVastaukset().add(viesti);
                    viestirepo.save(viesti);
                    viestirepo.save(mammaviesti);
                    accorepo.save(acco);
                    return "redirect:/user/" + username;
                }
                return "redirect:/user/" + username + "?error=You're replying to a comment that doesn't exist.";
            }
            return "redirect:/user/" + username + "?error=You're not permitted to comment on this person's wall.";
        }
        return "redirect:/user/" + authentication.getName() + "?error=No such user exists, so you're back to your own page now.";
    }

    @Secured("USER")
    @PostMapping("/user/{username}/comment/{id}/like")
    public String likePost(Authentication authentication, Model model,
            @PathVariable String username,
            @PathVariable Long id
    ) {
        Account acco = accorepo.findByUsername(username);
        Account viewer = accorepo.findByUsername(authentication.getName());
        if (acco != null && viewer != null) {
            if (acco.getKaverit().contains(viewer) || authentication.getName().equals(username)) {
                Viesti viesti = viestirepo.getOne(id);
                if (viesti.getLikersCom().contains(viewer)) {
                    return "redirect:/user/" + username + "?error=You can only like things once.";
                }
                viesti.getLikersCom().add(viewer);
                viestirepo.save(viesti);
                return "redirect:/user/" + username;
            }
            return "redirect:/user/" + username + "?error=You're not permitted to like comments on this person's wall.";
        }
        return "redirect:/user/" + authentication.getName() + "?error=No such user exists, so you're back to your own page now.";
    }

    @Secured("USER")
    @GetMapping("/user/{username}/friends")
    public String showFriends(Authentication authentication, Model model,
            @PathVariable String username
    ) {
        Account acco = accorepo.findByUsername(username);
        Account viewer = accorepo.findByUsername(authentication.getName());
        if (acco != null && viewer != null) {
            if (authentication.getName().equals(username)) {
                model.addAttribute("authentication", "user");
                model.addAttribute("account", acco);
                model.addAttribute("requests", acco.getFriendRequests());
                model.addAttribute("kaverit", acco.getKaverit());
                return "friendsandrequests";
            }
            return "redirect:/user/" + username + "?error=You're not permitted to look at other people's friends lists.";
        }
        return "redirect:/user/" + authentication.getName() + "?error=No such user exists, so you're back to your own page now.";
    }

    @Secured("USER")
    @PostMapping("/user/{username}/friends/accept")
    public String acceptRequest(Authentication authentication, Model model,
            @PathVariable String username
    ) {
        Account sender = accorepo.findByUsername(username);
        Account accepter = accorepo.findByUsername(authentication.getName());
        if (sender != null && accepter != null) {
            FriendRequest req = friendrepo.findByAccountAndSender(accepter, sender);
            friendrepo.delete(req);
            accepter.getFriendRequests().remove(req);
            accepter.getKaverit().add(sender);
            accorepo.save(accepter);
            sender.getKaverit().add(accepter);
            accorepo.save(sender);
            return "redirect:/user/" + accepter.getUsername() + "/friends";
        }
        return "redirect:/user/" + authentication.getName() + "?error=No such user exists, so you're back to your own page now.";
    }

    @Secured("USER")
    @PostMapping("/user/{username}/friends/reject")
    public String rejectRequest(Authentication authentication, Model model,
            @PathVariable String username
    ) {
        Account sender = accorepo.findByUsername(username);
        Account accepter = accorepo.findByUsername(authentication.getName());
        if (sender != null && accepter != null) {
            FriendRequest req = friendrepo.findByAccountAndSender(accepter, sender);
            friendrepo.delete(req);
            accepter.getFriendRequests().remove(req);
            accorepo.save(accepter);
            return "redirect:/user/" + accepter.getUsername() + "/friends";
        }
        return "redirect:/user/" + authentication.getName() + "?error=No such user exists, so you're back to your own page now.";
    }

    @Secured("USER")
    @PostMapping("/user/{username}/friendrequest")
    public String sendRequest(Authentication authentication, Model model,
            @PathVariable String username
    ) {
        Account acco = accorepo.findByUsername(username);
        Account sender = accorepo.findByUsername(authentication.getName());
        if (acco != null && sender != null) {
            FriendRequest req = new FriendRequest();
            req.setAccount(acco);
            req.setSender(sender);
            req.setDate(LocalDateTime.now());
            friendrepo.save(req);
            acco.getFriendRequests().add(req);
            accorepo.save(acco);
            return "redirect:/user/" + username;
        }
        return "redirect:/user/" + authentication.getName() + "?error=No such user exists, so you're back to your own page now.";
    }

    @Secured("USER")
    @PostMapping("/user/{username}/frienddelete")
    public String deleteFriend(Authentication authentication, Model model,
            @PathVariable String username
    ) {
        Account acco = accorepo.findByUsername(username);
        Account sender = accorepo.findByUsername(authentication.getName());
        if (acco != null && sender != null) {
            acco.getKaverit().remove(sender);
            accorepo.save(acco);
            sender.getKaverit().remove(acco);
            accorepo.save(sender);
            return "redirect:/user/" + username;
        }
        return "redirect:/user/" + authentication.getName() + "?error=No such user exists, so you're back to your own page now.";
    }

    @Secured("USER")
    @GetMapping("/search*")
    public String searchFunction(Authentication authentication, Model model,
            @RequestParam String filter
    ) {
        Account acco = accorepo.findByUsername(authentication.getName());
        List<Account> results = accorepo.findAll();
        results.removeIf(account -> !account.getName().toLowerCase().trim().contains(filter.toLowerCase().trim()));
        model.addAttribute("account", acco);
        model.addAttribute("results", results);
        return "searchresults";
    }
}
