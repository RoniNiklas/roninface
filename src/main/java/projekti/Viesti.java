package projekti;

import java.time.LocalDate;
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
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Viesti extends AbstractPersistable<Long> {

    @Id
    private Long id;
    @OneToOne(cascade = {CascadeType.ALL})
    private Account lahettaja;
    @ManyToOne
    private Account account;
    private String content;
    private LocalDateTime date;
    @OneToMany
    private List<Viesti> vastaukset = new ArrayList<>();
    @ManyToMany
    private List<Account> likersCom = new ArrayList<>();
}
