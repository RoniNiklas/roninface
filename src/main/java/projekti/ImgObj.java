package projekti;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.springframework.data.jpa.domain.AbstractPersistable;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
public class ImgObj extends AbstractPersistable<Long> {

    @Id
    Long id;
    @Lob
    // POISTA TÄN KOMMENTOINTI JOS/KUN UPLOADAAT HEROKUUN TAI MUUTEN EI POSTGRESQL TOIMI
    // TÄN KANSSA SE EI TOIMI LOCALHOSTIN H2 setupissa.
    //@Type(type="org.hibernate.type.BinaryType") 
    private byte[] content;
    private String caption;
    @ManyToOne
    private Account account;
    @OneToMany(mappedBy = "imgobj", cascade = CascadeType.ALL)
    private List<KuvaViesti> viestit;
    @ManyToMany
    private List<Account> likers = new ArrayList<>();
}
