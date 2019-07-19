package projekti;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account extends AbstractPersistable<Long> {

    @Id
    private Long id;
    private String username;
    private String password;
    private String name;
    private Long profilepic;
    @OneToMany(mappedBy = "account")
    private List<Viesti> viestit = new ArrayList<>();
    @OneToMany(mappedBy = "account")
    private List<ImgObj> kuvat = new ArrayList<>();
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> authorities;
    @ManyToMany
    private List<Account> kaverit = new ArrayList<>();
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FriendRequest> friendRequests = new ArrayList<>();
    @ManyToMany(mappedBy = "likers")
    private List<ImgObj> likedImages = new ArrayList<>();
    @ManyToMany(mappedBy = "likersCom")
    private List<Viesti> likedComments = new ArrayList<>();
    @ManyToMany(mappedBy = "likersImgCom")
    private List<KuvaViesti> likedImgComments = new ArrayList<>();

}
