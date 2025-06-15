package organizationmanagement.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    private List<Team> teams;

    @ElementCollection
    @CollectionTable(name = "department_users", joinColumns = @JoinColumn(name = "department_id"))
    @Column(name = "user_id")
    private Set<UUID> userIds = new HashSet<>();
}
