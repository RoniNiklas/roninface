package projekti;

import java.time.LocalDateTime;
import java.util.ArrayList;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.AbstractPersistable;
import java.util.List;
import java.util.Set;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KuvaViesti extends AbstractPersistable<Long> {

    @Id
    private Long id;
    @OneToOne
    private Account lahettaja;
    @ManyToOne
    private ImgObj imgobj;
    private String content;
    private LocalDateTime date;
    @OneToMany(cascade = {CascadeType.ALL})
    private List<KuvaViesti> vastaukset = new ArrayList<>();
    @ManyToMany
    private List<Account> likersImgCom = new ArrayList<>();
}
