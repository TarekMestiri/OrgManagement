package organizationmanagement.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @ElementCollection
    @CollectionTable(name = "team_users", joinColumns = @JoinColumn(name = "team_id"))
    @Column(name = "user_id")
    private Set<UUID> userIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "team_surveys", joinColumns = @JoinColumn(name = "team_id"))
    @Column(name = "survey_id")
    private Set<UUID> surveyIds = new HashSet<>();
}