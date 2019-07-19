package projekti;

import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.AbstractPersistable;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
public class FriendRequest extends AbstractPersistable<Long> {

    @Id
    Long id;
    private LocalDateTime date;
    @ManyToOne
    private Account sender;
    @ManyToOne
    private Account account;

}
