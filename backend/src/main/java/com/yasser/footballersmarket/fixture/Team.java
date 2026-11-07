package com.yasser.footballersmarket.fixture;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

// a club appearing in a discovered fixture. only teams referenced by a saved fixture are stored.
// upserted rather than insert-only: discovery is the single writer, so re-saving keeps a renamed
// team or a changed logo url current without clobbering anyone else's work
@Entity
@Table(name = "team")
public class Team {
    // rapid api team id, assigned from the fixtures response (no generation)
    @Id
    private Long id;
    private String name;
    private String logoUrl;

    public Team() {}

    public Team(Long id, String name, String logoUrl) {
        this.id = id;
        this.name = name;
        this.logoUrl = logoUrl;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }
}
