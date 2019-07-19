package projekti;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
 
public interface FriendRequestRepo extends JpaRepository<FriendRequest, Long> {
    FriendRequest findByAccountAndSender(@Param("account")Account acco, @Param("sender")Account sender);
}